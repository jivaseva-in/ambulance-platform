/**
 * Centralized API client for all Spring Boot backend communications
 */
import { state } from './state.js';

const BASE_URL = '/api/v1';

async function request(endpoint, options = {}) {
    const headers = options.headers || {};
    if (!headers['Content-Type'] && !(options.body instanceof FormData)) {
        headers['Content-Type'] = 'application/json';
    }

    const response = await fetch(`${BASE_URL}${endpoint}`, {
        ...options,
        headers
    });

    if (!response.ok) {
        const errText = await response.text();
        throw new Error(errText || `HTTP ${response.status}`);
    }

    // Return empty for void responses
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
        return await response.json();
    }
    return null;
}

export const api = {
    auth: {
        login: (username, password) =>
            request('/auth/login', {
                method: 'POST',
                body: JSON.stringify({ username, password })
            })
    },
    bookings: {
        createEmergency: (payload) =>
            request('/bookings', {
                method: 'POST',
                body: JSON.stringify(payload)
            }),
        getById: (id) =>
            request(`/bookings/${id}`),
        getAvailable: () =>
            request('/bookings/available', {
                headers: { 'Authorization': `Bearer ${state.driverToken}` }
            }),
        getActiveForDriver: () =>
            request('/bookings/driver/active', {
                headers: { 'Authorization': `Bearer ${state.driverToken}` }
            }),
        accept: (id) =>
            request(`/bookings/${id}/accept`, {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${state.driverToken}` }
            }),
        updateStatus: (id, status) =>
            request(`/bookings/${id}/status`, {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${state.driverToken}` },
                body: JSON.stringify({ status })
            }),
        getLocation: (id) =>
            request(`/bookings/${id}/location`),
        pushLocation: (id, latitude, longitude, speedKmh) =>
            request(`/bookings/${id}/location`, {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${state.driverToken}` },
                body: JSON.stringify({ latitude, longitude, speedKmh })
            })
    },
    dispatch: {
        logPhoneCall: (payload) =>
            request('/dispatch/phone-call', {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${state.adminToken}` },
                body: JSON.stringify(payload)
            })
    }
};
