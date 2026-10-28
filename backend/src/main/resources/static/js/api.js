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
        register: (payload) =>
            request('/auth/register', {
                method: 'POST',
                body: JSON.stringify(payload)
            }),
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
        confirm: (id) =>
            request(`/bookings/${id}/confirm`, {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${state.driverToken}` }
            }),
        failNegotiation: (id, reason = null) =>
            request(`/bookings/${id}/negotiation-failed${reason ? `?reason=${encodeURIComponent(reason)}` : ''}`, {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${state.driverToken}` }
            }),
        cancelRequest: (id, reason = null) =>
            request(`/bookings/${id}/cancel-request${reason ? `?reason=${encodeURIComponent(reason)}` : ''}`, {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${state.driverToken}` }
            }),
        requeue: (id) =>
            request(`/bookings/${id}/requeue`, {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${state.dispatcherToken || state.adminToken}` }
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
                headers: { 'Authorization': `Bearer ${state.dispatcherToken}` },
                body: JSON.stringify(payload)
            }),
        getOpenBookings: () =>
            request('/dispatch/bookings/open', {
                headers: { 'Authorization': `Bearer ${state.dispatcherToken}` }
            }),
        getAvailableDrivers: () =>
            request('/dispatch/drivers/available', {
                headers: { 'Authorization': `Bearer ${state.dispatcherToken}` }
            }),
        assignDriver: (bookingId, driverId) =>
            request(`/dispatch/bookings/${bookingId}/assign`, {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${state.dispatcherToken}` },
                body: JSON.stringify({ driverId })
            }),
        unassignDriver: (bookingId) =>
            request(`/dispatch/bookings/${bookingId}/unassign`, {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${state.dispatcherToken}` }
            }),
        getNearestDrivers: (pickupLatitude, pickupLongitude, destinationLatitude = pickupLatitude, destinationLongitude = pickupLongitude) =>
            request(`/dispatch/drivers/nearest?pickupLatitude=${encodeURIComponent(pickupLatitude)}&pickupLongitude=${encodeURIComponent(pickupLongitude)}&destinationLatitude=${encodeURIComponent(destinationLatitude)}&destinationLongitude=${encodeURIComponent(destinationLongitude)}`, {
                headers: { 'Authorization': `Bearer ${state.dispatcherToken}` }
            }),
    },
    admin: {
        getPendingUsers: () =>
            request('/admin/users/pending', {
                headers: { 'Authorization': `Bearer ${state.adminToken}` }
            }),
        approveUser: (userId) =>
            request(`/admin/users/${userId}/approve`, {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${state.adminToken}` }
            }),
        rejectUser: (userId) =>
            request(`/admin/users/${userId}/reject`, {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${state.adminToken}` }
            }),
        suspendUser: (userId) =>
            request(`/admin/users/${userId}/suspend`, {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${state.adminToken}` }
            }),
        reactivateUser: (userId) =>
            request(`/admin/users/${userId}/reactivate`, {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${state.adminToken}` }
            }),
        getAllUsers: () =>
            request('/admin/users', {
                headers: { 'Authorization': `Bearer ${state.adminToken}` }
            })
    },
    drivers: {
        getAvailability: () =>
            request('/drivers/me/availability', {
                headers: { 'Authorization': `Bearer ${state.driverToken}` }
            }),
        setAvailability: (availabilityStatus, latitude = null, longitude = null) =>
            request('/drivers/me/availability', {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${state.driverToken}` },
                body: JSON.stringify({ availabilityStatus, latitude, longitude })
            }),
        updateLocation: (latitude, longitude) =>
            request('/drivers/me/location', {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${state.driverToken}` },
                body: JSON.stringify({ latitude, longitude })
            })
    }
};
