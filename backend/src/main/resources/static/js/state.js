/**
 * Shared Application State and LocalStorage synchronization
 */
export const state = {
    driverToken: localStorage.getItem('driverToken') || null,
    driverName: localStorage.getItem('driverName') || null,
    dispatcherToken: localStorage.getItem('dispatcherToken') || null,
    dispatcherName: localStorage.getItem('dispatcherName') || null,
    dispatcherRole: localStorage.getItem('dispatcherRole') || null,
    adminToken: localStorage.getItem('adminToken') || null,
    adminName: localStorage.getItem('adminName') || null,
    adminRole: localStorage.getItem('adminRole') || null,
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

export function setAdminSession(token, fullName, role = null) {
    state.adminToken = token;
    state.adminName = fullName;
    state.adminRole = role;
    if (token) {
        localStorage.setItem('adminToken', token);
        localStorage.setItem('adminName', fullName);
        localStorage.setItem('adminRole', role || '');
    } else {
        localStorage.removeItem('adminToken');
        localStorage.removeItem('adminName');
        localStorage.removeItem('adminRole');
    }
}

export function setDispatcherSession(token, fullName, role = null) {
    state.dispatcherToken = token;
    state.dispatcherName = fullName;
    state.dispatcherRole = role;
    if (token) {
        localStorage.setItem('dispatcherToken', token);
        localStorage.setItem('dispatcherName', fullName);
        localStorage.setItem('dispatcherRole', role || '');
    } else {
        localStorage.removeItem('dispatcherToken');
        localStorage.removeItem('dispatcherName');
        localStorage.removeItem('dispatcherRole');
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
