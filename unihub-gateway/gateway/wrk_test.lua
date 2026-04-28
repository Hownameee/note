-- wrk_test.lua
local b = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_'
local function base64_encode(data)
    return ((data:gsub('.', function(x) 
        local r,b='',x:byte()
        for i=8,1,-1 do r=r..(b%2^i-b%2^(i-1)>0 and '1' or '0') end
        return r;
    end)..'0000'):gsub('%d%d%d?%d?%d?%d?', function(x)
        if (#x < 6) then return '' end
        local c=0
        for i=1,6 do c=c+(x:sub(i,i)=='1' and 2^(6-i) or 0) end
        return b:sub(c+1,c+1)
    end)..({ '', '', '' })[#data%3+1]) 
end

math.randomseed(os.time())

request = function()
    local user_id = math.random(1, 1000)
    local payload = '{"userId":"' .. user_id .. '"}'
    local b64_payload = base64_encode(payload)
    local jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." .. b64_payload .. ".sig"
    
    wrk.headers["Authorization"] = "Bearer " .. jwt
    return wrk.format("GET", "/api/test")
end