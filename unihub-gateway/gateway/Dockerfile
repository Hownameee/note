FROM openresty/openresty:alpine-fat

RUN luarocks install lua-resty-jwt

COPY nginx.conf /usr/local/openresty/nginx/conf/nginx.conf

RUN mkdir -p /etc/nginx/lua
COPY app.lua /etc/nginx/lua/app.lua