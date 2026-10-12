(function () {
    'use strict';

    const form = document.getElementById('loginForm');
    const errorEl = document.getElementById('loginError');
    const button = document.getElementById('loginBtn');

    // Already signed in? Skip the form. (plain fetch: a 401 here is normal and must not redirect)
    fetch('/api/auth/me', { credentials: 'same-origin' }).then(function (r) {
        if (r.ok) window.location.href = 'dashboard.html';
    }).catch(function () { /* stay on the login page */ });

    form.addEventListener('submit', async function (event) {
        event.preventDefault();
        errorEl.textContent = '';

        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value;
        if (!username || !password) {
            errorEl.textContent = 'Enter your username and password.';
            return;
        }

        button.disabled = true;
        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'X-XSRF-TOKEN': HMS.getCookie('XSRF-TOKEN') || ''
                },
                body: new URLSearchParams({ username: username, password: password }),
                credentials: 'same-origin'
            });
            if (response.ok) {
                window.location.href = 'dashboard.html';
                return;
            }
            errorEl.textContent = response.status === 401
                ? 'Invalid username or password.'
                : 'Login failed. Refresh the page and try again.';
        } catch (e) {
            errorEl.textContent = 'Cannot reach the server.';
        } finally {
            button.disabled = false;
        }
    });
})();
