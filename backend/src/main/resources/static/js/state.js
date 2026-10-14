/**
 * Shared Application State and LocalStorage synchronization
 */
export const state = {
    driverToken: localStorage.getItem('driverToken') || null,
    driverName: localStorage.getItem('driverName') || null,
    adminToken: localStorage.getItem('adminToken') || null,
    adminName: localStorage.getItem('adminName') || null,
    driverActiveTrip: JSON.parse(localStorage.getItem('driverActiveTrip') || 'null'),
    activeBookingId: null,
    trackingInterval: null,
    pickerMap: null,
    pickerMarker: null,
    trackMap: null,
    trackAmbulanceMarker: null,
    trackPatientMarker: null
};

export function setDriverSession(token, fullName) {
    state.driverToken = token;
    state.driverName = fullName;
    if (token) {
        localStorage.setItem('driverToken', token);
        localStorage.setItem('driverName', fullName);
    } else {
        localStorage.removeItem('driverToken');
        localStorage.removeItem('driverName');
        state.driverActiveTrip = null;
        localStorage.removeItem('driverActiveTrip');
    }
}

export function setAdminSession(token, fullName) {
    state.adminToken = token;
    state.adminName = fullName;
    if (token) {
        localStorage.setItem('adminToken', token);
        localStorage.setItem('adminName', fullName);
    } else {
        localStorage.removeItem('adminToken');
        localStorage.removeItem('adminName');
    }
}

export function setDriverActiveTrip(trip) {
    state.driverActiveTrip = trip;
    if (trip) {
        localStorage.setItem('driverActiveTrip', JSON.stringify(trip));
    } else {
        localStorage.removeItem('driverActiveTrip');
    }
}
