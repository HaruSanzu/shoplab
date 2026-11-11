// Where the website finds the backend API.
// The browser runs on YOUR computer, so "localhost:8080" means "port 8080 on this computer",
// which is where docker-compose publishes the backend.
// To use another backend, change apiUrl here. No change in index.html is needed.
window.SHOPLAB_CONFIG = {
  apiUrl: 'http://localhost:8080'
};
