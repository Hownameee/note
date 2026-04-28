local jwt = require "resty.jwt"
local redis = require "resty.redis"
local cjson = require "cjson"

local function get_jwt_payload(auth_header)
    if not auth_header then
        return nil, "Missing Authorization header"
    end

    local token = string.match(auth_header, "Bearer%s+(.+)")
    if not token then
        return nil, "Invalid Authorization header format"
    end

    local jwt_obj = jwt:load_jwt(token)

    if not jwt_obj or not jwt_obj.payload then
        return nil, "Invalid JWT structure"
    end

    return jwt_obj.payload, nil
end

local function check_rate_limit(red, user_id)
    local capacity = 20      -- Max token
    local refill_rate = 5   -- Refill per second
    local now = ngx.now()   -- Current Nginx timestamp
    local key = "rate_limit:user:" .. user_id

    -- This Lua script runs ATOMICALLY inside Redis
    local script = [[
        local key = KEYS[1]
        local cap = tonumber(ARGV[1])
        local rate = tonumber(ARGV[2])
        local now = tonumber(ARGV[3])
        
        -- Get current bucket state
        local data = redis.call('HMGET', key, 'tokens', 'last_time')
        local last_tokens = tonumber(data[1]) or cap
        local last_time = tonumber(data[2]) or now
        
        -- Calculate tokens added based on time passed
        local delta = math.max(0, now - last_time) * rate
        local current_tokens = math.min(cap, last_tokens + delta)
        
        if current_tokens >= 1 then
            -- update new token
            local new_tokens = current_tokens - 1
            redis.call('HMSET', key, 'tokens', new_tokens, 'last_time', now)
            
            -- set expired time
            local seconds_to_full = math.ceil((cap - new_tokens) / rate)
            redis.call('EXPIRE', key, seconds_to_full + 1) 
            return 1
        else
            return 0
        end
    ]]

    -- Execute the script in Redis
    local res, err = red:eval(script, 1, key, capacity, refill_rate, now)
    return res == 1, err
end

-- decode json
local auth_header = ngx.var.http_authorization
local payload, err = get_jwt_payload(auth_header)
ngx.default_type = 'application/json'

if err then
    ngx.status = 401
    ngx.say(cjson.encode({ error = err }))
    return ngx.exit(401)
end

if not payload.userId then
    ngx.status = 400
    ngx.say(cjson.encode({ error = "JWT Payload is missing 'userId'" }))
    return ngx.exit(400)
end

local safe_user_id = tostring(payload.userId)

-- connect redis
local red = redis:new()
red:set_timeout(1000)
local ok, conn_err = red:connect("redis", 6379)

if not ok then
    ngx.status = 500
    ngx.say(cjson.encode({ error = "Failed to connect to Redis: " .. (conn_err or "unknown") }))
    return ngx.exit(500)
end

-- check rate limit
local allowed, script_err = check_rate_limit(red, safe_user_id)
red:set_keepalive(10000, 100) 

-- redis err
if script_err then
    ngx.status = 500
    ngx.say(cjson.encode({ error = "Redis Script Error: " .. script_err }))
    return ngx.exit(500)
end

-- user limit
if not allowed then
    ngx.status = 429
    ngx.say(cjson.encode({ 
        error = "Too Many Requests", 
        message = "Calm down, User " .. safe_user_id .. "!" 
    }))
    return ngx.exit(429)
end

ngx.req.set_header("X-User-Id", safe_user_id)
return