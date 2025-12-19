/**
 * Session Management Utility for Ciyex Frontend
 * Handles automatic token refresh and session timeout
 */
class SessionManager {
    constructor(options = {}) {
        this.apiBaseUrl = options.apiBaseUrl || 'http://localhost:8080/api';
        this.sessionTimeoutMinutes = options.sessionTimeoutMinutes || 25;
        this.warningMinutes = options.warningMinutes || 5;
        this.keepAliveInterval = options.keepAliveInterval || 60000; // 1 minute

        this.lastActivity = Date.now();
        this.sessionTimer = null;
        this.warningTimer = null;
        this.keepAliveTimer = null;
        this.isWarningShown = false;

        this.init();
    }

    init() {
        this.setupActivityListeners();
        this.startKeepAlive();
        this.resetSessionTimer();
    }

    setupActivityListeners() {
        const events = ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart', 'click'];

        events.forEach(event => {
            document.addEventListener(event, () => {
                this.updateActivity();
            }, true);
        });
    }

    updateActivity() {
        this.lastActivity = Date.now();

        if (this.isWarningShown) {
            this.hideSessionWarning();
        }

        this.resetSessionTimer();
    }

    resetSessionTimer() {
        this.clearTimers();

        const sessionTimeoutMs = this.sessionTimeoutMinutes * 60 * 1000;
        const warningTimeoutMs = (this.sessionTimeoutMinutes - this.warningMinutes) * 60 * 1000;

        // Set warning timer
        this.warningTimer = setTimeout(() => {
            this.showSessionWarning();
        }, warningTimeoutMs);

        // Set session expiry timer
        this.sessionTimer = setTimeout(() => {
            this.handleSessionExpired();
        }, sessionTimeoutMs);
    }

    clearTimers() {
        if (this.sessionTimer) {
            clearTimeout(this.sessionTimer);
            this.sessionTimer = null;
        }

        if (this.warningTimer) {
            clearTimeout(this.warningTimer);
            this.warningTimer = null;
        }
    }

    startKeepAlive() {
        this.keepAliveTimer = setInterval(() => {
            this.sendKeepAlive();
        }, this.keepAliveInterval);
    }

    async sendKeepAlive() {
        try {
            const token = this.getAuthToken();
            if (!token) return;

            const response = await fetch(`${this.apiBaseUrl}/session-management/keep-alive`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.status === 401) {
                // Try to refresh token before expiring session
                const refreshed = await this.refreshToken();
                if (!refreshed) {
                    this.handleSessionExpired();
                }
            }
        } catch (error) {
            console.error('Keep-alive request failed:', error);
        }
    }

    async refreshToken() {
        try {
            const refreshToken = this.getRefreshToken();
            if (!refreshToken) return false;

            const response = await fetch(`${this.apiBaseUrl}/auth/refresh`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ refreshToken })
            });

            if (response.ok) {
                const data = await response.json();
                this.setAuthToken(data.access_token);
                this.setRefreshToken(data.refresh_token);
                console.log('Token refreshed successfully');
                return true;
            }
        } catch (error) {
            console.error('Token refresh failed:', error);
        }
        return false;
    }

    showSessionWarning() {
        this.isWarningShown = true;

        // Create warning modal
        const modal = document.createElement('div');
        modal.id = 'session-warning-modal';
        modal.className = 'session-warning-modal';
        modal.innerHTML = `
            <div class="session-warning-content">
                <h3>Session Expiring Soon</h3>
                <p>Your session will expire in ${this.warningMinutes} minutes due to inactivity.</p>
                <p>Click "Stay Logged In" to continue your session.</p>
                <div class="session-warning-buttons">
                    <button id="stay-logged-in" class="btn btn-primary">Stay Logged In</button>
                    <button id="logout-now" class="btn btn-secondary">Logout Now</button>
                </div>
            </div>
        `;

        // Add styles
        const style = document.createElement('style');
        style.textContent = `
            .session-warning-modal {
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0, 0, 0, 0.5);
                display: flex;
                justify-content: center;
                align-items: center;
                z-index: 10000;
            }
            .session-warning-content {
                background: white;
                padding: 20px;
                border-radius: 8px;
                text-align: center;
                max-width: 400px;
                box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            }
            .session-warning-buttons {
                margin-top: 20px;
                display: flex;
                gap: 10px;
                justify-content: center;
            }
            .btn {
                padding: 8px 16px;
                border: none;
                border-radius: 4px;
                cursor: pointer;
            }
            .btn-primary {
                background: #007bff;
                color: white;
            }
            .btn-secondary {
                background: #6c757d;
                color: white;
            }
        `;

        document.head.appendChild(style);
        document.body.appendChild(modal);

        // Add event listeners
        document.getElementById('stay-logged-in').addEventListener('click', () => {
            this.updateActivity();
            this.hideSessionWarning();
        });

        document.getElementById('logout-now').addEventListener('click', () => {
            this.handleSessionExpired();
        });
    }

    hideSessionWarning() {
        this.isWarningShown = false;
        const modal = document.getElementById('session-warning-modal');
        if (modal) {
            modal.remove();
        }
    }

    handleSessionExpired() {
        this.clearTimers();

        if (this.keepAliveTimer) {
            clearInterval(this.keepAliveTimer);
        }

        // Clear auth token
        this.clearAuthToken();

        // Show expiry message
        alert('Your session has expired due to inactivity. You will be redirected to the login page.');

        // Redirect to login
        window.location.href = '/login';
    }

    async updateSessionTimeout(practiceId, timeoutMinutes) {
        try {
            const token = this.getAuthToken();
            if (!token) throw new Error('No auth token');

            const response = await fetch(`${this.apiBaseUrl}/session-management/timeout-settings/${practiceId}`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    sessionTimeoutMinutes: timeoutMinutes
                })
            });

            if (response.ok) {
                this.sessionTimeoutMinutes = timeoutMinutes;
                this.resetSessionTimer();
                return true;
            } else {
                throw new Error('Failed to update session timeout');
            }
        } catch (error) {
            console.error('Error updating session timeout:', error);
            return false;
        }
    }

    async getSessionTimeoutSettings(practiceId) {
        try {
            const token = this.getAuthToken();
            if (!token) throw new Error('No auth token');

            const response = await fetch(`${this.apiBaseUrl}/session-management/timeout-settings/${practiceId}`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                const result = await response.json();
                return result.data;
            } else {
                throw new Error('Failed to get session timeout settings');
            }
        } catch (error) {
            console.error('Error getting session timeout settings:', error);
            return null;
        }
    }

    getAuthToken() {
        return localStorage.getItem('authToken') || sessionStorage.getItem('authToken');
    }

    getRefreshToken() {
        return localStorage.getItem('refreshToken') || sessionStorage.getItem('refreshToken');
    }

    setAuthToken(token) {
        if (localStorage.getItem('authToken')) {
            localStorage.setItem('authToken', token);
        } else {
            sessionStorage.setItem('authToken', token);
        }
    }

    setRefreshToken(token) {
        if (localStorage.getItem('refreshToken')) {
            localStorage.setItem('refreshToken', token);
        } else {
            sessionStorage.setItem('refreshToken', token);
        }
    }

    clearAuthToken() {
        localStorage.removeItem('authToken');
        localStorage.removeItem('refreshToken');
        sessionStorage.removeItem('authToken');
        sessionStorage.removeItem('refreshToken');
    }

    destroy() {
        this.clearTimers();

        if (this.keepAliveTimer) {
            clearInterval(this.keepAliveTimer);
        }

        this.hideSessionWarning();
    }
}

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = SessionManager;
} else {
    window.SessionManager = SessionManager;
}