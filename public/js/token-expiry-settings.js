/**
 * Token Expiry Settings Component
 * Manages customizable token expiration time for different organizations
 */
class TokenExpirySettings {
    constructor(containerId, apiBaseUrl = '/api') {
        this.container = document.getElementById(containerId);
        this.apiBaseUrl = apiBaseUrl;
        this.currentExpiry = 5; // Default 5 minutes
        this.init();
    }

    init() {
        this.render();
        this.loadCurrentSettings();
        this.attachEventListeners();
    }

    render() {
        this.container.innerHTML = `
            <div class="token-expiry-settings">
                <div class="settings-header">
                    <h3>Session Token Settings</h3>
                    <p class="settings-description">
                        Configure how long user sessions remain active before requiring re-authentication.
                    </p>
                </div>
                
                <div class="settings-form">
                    <div class="form-group">
                        <label for="tokenExpiry">Session Timeout (minutes)</label>
                        <div class="input-group">
                            <input 
                                type="range" 
                                id="tokenExpiry" 
                                min="5" 
                                max="30" 
                                step="1" 
                                value="5"
                                class="range-input"
                            />
                            <span class="range-value" id="tokenExpiryValue">5 minutes</span>
                        </div>
                        <div class="range-labels">
                            <span>5 min</span>
                            <span>30 min</span>
                        </div>
                        <small class="help-text">
                            Choose between 5-30 minutes. Shorter times are more secure but require more frequent logins.
                        </small>
                    </div>
                    
                    <div class="form-actions">
                        <button type="button" id="saveSettings" class="btn btn-primary">
                            <span class="btn-text">Save Settings</span>
                            <span class="btn-loading" style="display: none;">Saving...</span>
                        </button>
                        <button type="button" id="resetSettings" class="btn btn-secondary">
                            Reset to Default
                        </button>
                    </div>
                    
                    <div class="status-message" id="statusMessage" style="display: none;"></div>
                </div>
                
                <div class="keycloak-status">
                    <div class="status-indicator" id="keycloakStatus">
                        <span class="status-dot"></span>
                        <span class="status-text">Keycloak Status: Unknown</span>
                    </div>
                </div>
            </div>
        `;
    }

    attachEventListeners() {
        const rangeInput = document.getElementById('tokenExpiry');
        const rangeValue = document.getElementById('tokenExpiryValue');
        const saveButton = document.getElementById('saveSettings');
        const resetButton = document.getElementById('resetSettings');

        // Update display value when range changes
        rangeInput.addEventListener('input', (e) => {
            const minutes = parseInt(e.target.value);
            rangeValue.textContent = `${minutes} minute${minutes !== 1 ? 's' : ''}`;
        });

        // Save settings
        saveButton.addEventListener('click', () => {
            this.saveSettings();
        });

        // Reset to default
        resetButton.addEventListener('click', () => {
            this.resetToDefault();
        });
    }

    async loadCurrentSettings() {
        try {
            this.showStatus('Loading current settings...', 'info');
            
            const response = await fetch(`${this.apiBaseUrl}/settings/token-expiry`);
            const result = await response.json();
            
            if (result.success && result.data) {
                this.currentExpiry = result.data.minutes;
                this.updateUI(this.currentExpiry);
                this.updateKeycloakStatus(result.data.keycloakUpdated, result.data.keycloakMessage);
                this.showStatus('Settings loaded successfully', 'success');
            } else {
                throw new Error(result.message || 'Failed to load settings');
            }
        } catch (error) {
            console.error('Error loading settings:', error);
            this.showStatus('Failed to load current settings', 'error');
        }
    }

    async saveSettings() {
        const rangeInput = document.getElementById('tokenExpiry');
        const saveButton = document.getElementById('saveSettings');
        const minutes = parseInt(rangeInput.value);

        try {
            // Show loading state
            this.setButtonLoading(saveButton, true);
            this.showStatus('Updating token expiry settings...', 'info');

            const response = await fetch(`${this.apiBaseUrl}/settings/token-expiry`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ minutes })
            });

            const result = await response.json();

            if (result.success) {
                this.currentExpiry = minutes;
                this.updateKeycloakStatus(result.data.keycloakUpdated, result.data.keycloakMessage);
                
                let message = `Token expiry updated to ${minutes} minutes`;
                if (!result.data.keycloakUpdated) {
                    message += ' (Keycloak update failed - check logs)';
                }
                
                this.showStatus(message, result.data.keycloakUpdated ? 'success' : 'warning');
            } else {
                throw new Error(result.message || 'Failed to update settings');
            }
        } catch (error) {
            console.error('Error saving settings:', error);
            this.showStatus(`Failed to save settings: ${error.message}`, 'error');
        } finally {
            this.setButtonLoading(saveButton, false);
        }
    }

    resetToDefault() {
        const rangeInput = document.getElementById('tokenExpiry');
        const rangeValue = document.getElementById('tokenExpiryValue');
        
        rangeInput.value = 5;
        rangeValue.textContent = '5 minutes';
        
        this.showStatus('Reset to default (5 minutes). Click Save to apply.', 'info');
    }

    updateUI(minutes) {
        const rangeInput = document.getElementById('tokenExpiry');
        const rangeValue = document.getElementById('tokenExpiryValue');
        
        rangeInput.value = minutes;
        rangeValue.textContent = `${minutes} minute${minutes !== 1 ? 's' : ''}`;
    }

    updateKeycloakStatus(isUpdated, message) {
        const statusElement = document.getElementById('keycloakStatus');
        const statusDot = statusElement.querySelector('.status-dot');
        const statusText = statusElement.querySelector('.status-text');
        
        statusDot.className = `status-dot ${isUpdated ? 'success' : 'error'}`;
        statusText.textContent = `Keycloak Status: ${message || (isUpdated ? 'Connected' : 'Error')}`;
    }

    showStatus(message, type = 'info') {
        const statusElement = document.getElementById('statusMessage');
        statusElement.textContent = message;
        statusElement.className = `status-message ${type}`;
        statusElement.style.display = 'block';
        
        // Auto-hide after 5 seconds for success/info messages
        if (type === 'success' || type === 'info') {
            setTimeout(() => {
                statusElement.style.display = 'none';
            }, 5000);
        }
    }

    setButtonLoading(button, isLoading) {
        const btnText = button.querySelector('.btn-text');
        const btnLoading = button.querySelector('.btn-loading');
        
        if (isLoading) {
            btnText.style.display = 'none';
            btnLoading.style.display = 'inline';
            button.disabled = true;
        } else {
            btnText.style.display = 'inline';
            btnLoading.style.display = 'none';
            button.disabled = false;
        }
    }
}

// CSS Styles (can be moved to a separate CSS file)
const styles = `
<style>
.token-expiry-settings {
    max-width: 600px;
    margin: 0 auto;
    padding: 20px;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.settings-header {
    margin-bottom: 30px;
}

.settings-header h3 {
    margin: 0 0 10px 0;
    color: #333;
    font-size: 24px;
}

.settings-description {
    color: #666;
    margin: 0;
    line-height: 1.5;
}

.form-group {
    margin-bottom: 25px;
}

.form-group label {
    display: block;
    margin-bottom: 10px;
    font-weight: 600;
    color: #333;
}

.input-group {
    display: flex;
    align-items: center;
    gap: 15px;
    margin-bottom: 10px;
}

.range-input {
    flex: 1;
    height: 6px;
    background: #ddd;
    border-radius: 3px;
    outline: none;
    -webkit-appearance: none;
}

.range-input::-webkit-slider-thumb {
    -webkit-appearance: none;
    width: 20px;
    height: 20px;
    background: #007bff;
    border-radius: 50%;
    cursor: pointer;
}

.range-input::-moz-range-thumb {
    width: 20px;
    height: 20px;
    background: #007bff;
    border-radius: 50%;
    cursor: pointer;
    border: none;
}

.range-value {
    min-width: 80px;
    font-weight: 600;
    color: #007bff;
}

.range-labels {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    color: #666;
    margin-bottom: 5px;
}

.help-text {
    color: #666;
    font-size: 14px;
    line-height: 1.4;
}

.form-actions {
    display: flex;
    gap: 10px;
    margin-bottom: 20px;
}

.btn {
    padding: 10px 20px;
    border: none;
    border-radius: 5px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 600;
    transition: background-color 0.2s;
}

.btn-primary {
    background: #007bff;
    color: white;
}

.btn-primary:hover:not(:disabled) {
    background: #0056b3;
}

.btn-secondary {
    background: #6c757d;
    color: white;
}

.btn-secondary:hover {
    background: #545b62;
}

.btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.status-message {
    padding: 10px 15px;
    border-radius: 5px;
    margin-bottom: 15px;
    font-size: 14px;
}

.status-message.success {
    background: #d4edda;
    color: #155724;
    border: 1px solid #c3e6cb;
}

.status-message.error {
    background: #f8d7da;
    color: #721c24;
    border: 1px solid #f5c6cb;
}

.status-message.warning {
    background: #fff3cd;
    color: #856404;
    border: 1px solid #ffeaa7;
}

.status-message.info {
    background: #d1ecf1;
    color: #0c5460;
    border: 1px solid #bee5eb;
}

.keycloak-status {
    padding: 15px;
    background: #f8f9fa;
    border-radius: 5px;
    border: 1px solid #dee2e6;
}

.status-indicator {
    display: flex;
    align-items: center;
    gap: 10px;
}

.status-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
}

.status-dot.success {
    background: #28a745;
}

.status-dot.error {
    background: #dc3545;
}

.status-text {
    font-size: 14px;
    color: #333;
}
</style>
`;

// Inject styles
document.head.insertAdjacentHTML('beforeend', styles);

// Export for use
window.TokenExpirySettings = TokenExpirySettings;