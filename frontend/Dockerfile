# The frontend is a static page (HTML + JS), so a plain web server is all it needs.
# nginx serves every file in /usr/share/nginx/html.
FROM nginx:1.27-alpine
COPY index.html config.js /usr/share/nginx/html/
EXPOSE 80
