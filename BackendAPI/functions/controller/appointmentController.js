const admin = require('../config/db.js'); 

// // Book an appointment
// const bookAppointment = async (req, res) => {
//     const { date, dentist, dentistId, description, slot, status } = req.body;
//     //const userId = req.userId ; 

//     const userId = req.body.userId || req.user?.id || req.user?.clientId || req.body.clientId;


//     if (!userId) {
//         console.error("User ID is missing from request.");
//         return res.status(400).json({ message: 'User not authenticated' });
//     }

//     try {
//         const appointmentId = admin.database().ref('appointments').push().key;

//         const appointment = {
//             appointmentId,
//             date,
//             dentist,
//             dentistId,
//             description,
//             slot,
//             userId,
//             status: 'pending',
//             createdAt: admin.database.ServerValue.TIMESTAMP,
//         };

//         await admin.database().ref(`appointments/${appointmentId}`).set(appointment);

//         res.status(201).json({
//             message: 'Appointment booked successfully',
//             appointmentId,
//         });
//     } catch (error) {
//         console.error("Error booking appointment:", error);
//         res.status(500).json({ message: 'Server error' });
//     }
// };

const bookAppointment = async (req, res) => {
    const { date, dentist, dentistId, description, slot, status, clientUsername } = req.body; // added clientUsername

    const userId = req.body.userId || req.user?.id || req.user?.clientId || req.body.clientId;

    if (!userId) {
        console.error("User ID is missing from request.");
        return res.status(400).json({ message: 'User not authenticated' });
    }

    try {
        const appointmentId = admin.database().ref('appointments').push().key;

        const appointment = {
            appointmentId,
            date,
            dentist,
            dentistId,
            description,
            slot,
            clientUsername, // added clientUsername to appointment object
            userId,
            status: 'pending',
            createdAt: admin.database.ServerValue.TIMESTAMP,
        };

        await admin.database().ref(`appointments/${appointmentId}`).set(appointment);

        res.status(201).json({
            message: 'Appointment booked successfully',
            appointmentId,
        });
    } catch (error) {
        console.error("Error booking appointment:", error);
        res.status(500).json({ message: 'Server error' });
    }
};


// Reschedule an appointment
const rescheduleAppointment = async (req, res) => {
    const appointmentId = req.params.appointmentId.trim();
    const { date, description, slot} = req.body;

    try {
        const appointmentRef = admin.database().ref(`appointments/${appointmentId}`);
        const appointmentSnapshot = await appointmentRef.once('value');

        if (!appointmentSnapshot.exists()) {
            return res.status(404).json({ message: 'Appointment not found' });
        }

        const appointment = appointmentSnapshot.val();

        // // Check if the logged-in user is allowed to reschedule this appointment
        // if (appointment.userId !== req.user.id) {
        //     return res.status(403).json({ message: 'You do not have permission to reschedule this appointment' });
        // }

        // Update the appointment details and mark it as rescheduled
        await appointmentRef.update({
            date: date || appointment.date,
            slot: slot || appointment.slot,
            description: description || appointment.description,
            status: 'rescheduled', // Change status to rescheduled
            updatedAt: admin.database.ServerValue.TIMESTAMP, // Add timestamp
        });

        res.status(200).json({
            message: 'Appointment rescheduled successfully',
            appointmentId,
        });
    } catch (error) {
        console.error('Error rescheduling appointment:', error);
        res.status(500).json({ message: 'Server error' });
    }
};

// // Cancel an appointment
// const cancelAppointment = async (req, res) => {
//     const appointmentId = req.params.appointmentId.trim();

//     try {
//         const appointmentRef = admin.database().ref(`appointments/${appointmentId}`);
//         const appointmentSnapshot = await appointmentRef.once('value');

//         if (!appointmentSnapshot.exists()) {
//            return res.status(404).json({ message: 'Appointment not found' });
           
//         }

       

//         console.log("Trying to cancel appointment with ID:", appointmentId);
//         console.log("Appointment exists:", appointmentSnapshot.exists());

//         const appointment = appointmentSnapshot.val();

//         // // Check if the logged-in user is the patient or a staff member
//         // if (appointment.userId !== req.user.id && req.user.role !== 'staff') {
//         //     return res.status(403).json({ message: 'You do not have permission to cancel this appointment' });
//         // }

//         // Mark the appointment as canceled instead of deleting it
//         await appointmentRef.update({
//             status: 'canceled',
//             updatedAt: admin.database.ServerValue.TIMESTAMP, // Add timestamp
//         });

        

//         res.status(200).json({ message: 'Appointment canceled successfully' });


  

//     } catch (error) {
//         console.error('Error canceling appointment:', error);
//         res.status(500).json({ message: 'Server error' });
//     }
// };

// Cancel an appointment
const cancelAppointment = async (req, res) => {
    const appointmentId = req.params.appointmentId?.trim();

    console.log("Received appointment ID to cancel:", appointmentId);

    if (!appointmentId) {
        console.log("No valid appointment ID provided.");
        return res.status(400).json({ message: 'Invalid appointment ID' });
    }

    try {
        const appointmentRef = admin.database().ref(`appointments/${appointmentId}`);
        console.log(`Attempting to look up appointment with ID: ${appointmentId}`);

        const appointmentSnapshot = await appointmentRef.once('value');
        
        if (!appointmentSnapshot.exists()) {
            console.log(`Appointment with ID: ${appointmentId} not found.`);
            return res.status(404).json({ message: 'Appointment not found' });
        }

        console.log("Appointment found, proceeding to cancel.");
        const appointment = appointmentSnapshot.val();

        // Optional: Uncomment this section to check user permissions
        // if (appointment.userId !== req.user.id && req.user.role !== 'staff') {
        //     console.log("Permission denied for user ID:", req.user?.id);
        //     return res.status(403).json({ message: 'You do not have permission to cancel this appointment' });
        // }

        // Update appointment status to 'canceled'
        console.log("Updating appointment status to 'canceled'.");
        await appointmentRef.update({
            status: 'canceled',
            updatedAt: admin.database.ServerValue.TIMESTAMP,
        });

        console.log("Appointment successfully canceled.");
        res.status(200).json({ message: 'Appointment canceled successfully' });

    } catch (error) {
        console.error('Error canceling appointment:', error);
        res.status(500).json({ message: 'Server error' });
    }
};



// Approve an appointment
const approveAppointment = async (req, res) => {
    const appointmentId = req.params.appointmentId.trim();

    try {
        const appointmentRef = admin.database().ref(`appointments/${appointmentId}`);
        const appointmentSnapshot = await appointmentRef.once('value');

        console.log('Incoming appointmentId:', appointmentId);

        if (!appointmentSnapshot.exists()) {
            console.log(`Appointment not found for ID: ${appointmentId}`);
            return res.status(404).json({ message: 'Appointment not found' });
        }

        const appointment = appointmentSnapshot.val();

        // Check if the appointment is already approved
        if (appointment.status === 'approved') {
            return res.status(400).json({ message: 'This appointment is already confirmed' });
        }

        // Check if the appointment is canceled
        if (appointment.status === 'canceled') {
            return res.status(400).json({ message: 'This appointment has been canceled and cannot be approved' });
        }

        // // Verify if the logged-in user is a staff member
        // if (req.user.role !== 'staff') {
        //     return res.status(403).json({ message: 'You do not have permission to approve this appointment' });
        // }

        // Update the appointment status to 'approved'
        await appointmentRef.update({
            status: 'approved',
            updatedAt: admin.database.ServerValue.TIMESTAMP, // Add timestamp
        });

        res.status(200).json({
            message: 'Appointment approved successfully',
            appointmentId,
        });
    } catch (error) {
        console.error('Error approving appointment:', error);
        res.status(500).json({ message: 'Server error' });
    }
};

const getPatientNotifications = async (req, res) => {
    try {
        const userId = req.params.userId || req.user.id;
        const now = Date.now();
        const oneDayLater = now + 24 * 60 * 60 * 1000;
        const fcmToken = req.query.fcm_token;

        if (!fcmToken) {
            return res.status(400).json({ message: 'FCM token is required' });
        }

        const notifications = [];

        // Fetch and handle upcoming appointments with `Promise.all`
        const upcomingAppointmentsSnapshot = await admin.database().ref('appointments')
            .orderByChild('userId')
            .equalTo(userId)
            .once('value');

        const upcomingPromises = [];
        upcomingAppointmentsSnapshot.forEach((doc) => {
            const appointment = doc.val();
            if (appointment.date >= now && appointment.date < oneDayLater && appointment.status === 'approved') {
                const notification = {
                    appointmentId: doc.key,
                    message: `Reminder: You have a confirmed appointment tomorrow at ${appointment.time}.`,
                    date: appointment.date,
                    time: appointment.time,
                    description: appointment.description,
                    status: appointment.status
                };

                notifications.push(notification);
                upcomingPromises.push(
                    admin.messaging().send({
                        token: fcmToken,
                        notification: {
                            title: "Appointment Reminder",
                            body: notification.message,
                        },
                        data: {
                            appointmentId: String(doc.key),
                            time: String(appointment.time),
                            description: String(appointment.description)
                        }
                    }).catch(error => {
                        console.error("Error sending reminder notification:", error);
                    })
                );
            }
        });

        // Fetch and handle status-changed appointments with `Promise.all`
        const statusChangedAppointmentsSnapshot = await admin.database().ref('appointments')
            .orderByChild('userId')
            .equalTo(userId)
            .once('value');

        const statusPromises = [];
        statusChangedAppointmentsSnapshot.forEach((doc) => {
            const appointment = doc.val();
            if (['rescheduled', 'canceled', 'approved'].includes(appointment.status)) {
                let message;
                if (appointment.status === 'rescheduled') {
                    message = `Your appointment for ${appointment.description} on this date ${appointment.date} has been rescheduled.`;
                } else if (appointment.status === 'canceled') {
                    message = `Your appointment for ${appointment.description} on this date ${appointment.date} has been canceled.`;
                } else if (appointment.status === 'approved') {
                    message = `Your appointment for ${appointment.description} on this date ${appointment.date} has been confirmed.`;
                }

                const notification = {
                    appointmentId: doc.key,
                    message,
                    date: appointment.date,
                    time: appointment.time,
                    description: appointment.description,
                    status: appointment.status
                };

                notifications.push(notification);
                statusPromises.push(
                    admin.messaging().send({
                        token: fcmToken,
                        notification: {
                            title: "Appointment Update",
                            body: message,
                        },
                        data: {
                            appointmentId: String(doc.key),
                            time: String(appointment.time),
                            description: String(appointment.description)
                        }
                    }).catch(error => {
                        console.error("Error sending status update notification:", error);
                    })
                );
            }
        });

        // Await all promises to complete sending notifications
        await Promise.all([...upcomingPromises, ...statusPromises]);

        const notificationCount = notifications.length;
        if (notificationCount === 0) {
            return res.status(404).json({ message: 'No notifications found for this patient' });
        }

        res.status(200).json({ count: notificationCount, notifications });
    } catch (error) {
        console.error('Error fetching patient notifications:', error);
        res.status(500).json({ message: 'Server error' });
    }
};

// Get staff notifications
const getStaffNotifications = async (req, res) => {
    try {
        const now = new Date();
        const oneDayLater = new Date(now);
        oneDayLater.setDate(now.getDate() + 1);

        // Fetch all upcoming appointments within the next 24 hours with confirmed status
        const upcomingAppointments = await Appointment.find({
            status: 'confirmed',
            date: { $gte: now, $lt: oneDayLater }
        }).populate('patient', 'name email'); // Populate patient details

        // Fetch appointments with status changes (pending, rescheduled, canceled)
        const statusChangedAppointments = await Appointment.find({
            status: { $in: ['pending', 'rescheduled', 'canceled'] }
        }).populate('patient', 'name email'); // Populate patient details

        // Prepare notifications for upcoming confirmed appointments
        const upcomingNotifications = upcomingAppointments.map(appointment => ({
            appointmentId: appointment._id,
            patientId: appointment.patient._id,
            message: `Reminder: ${appointment.patient.name} has a confirmed appointment tomorrow at ${appointment.time}.`,
            date: appointment.date,
            time: appointment.time,
            description: appointment.description,
            status: appointment.status
        }));

        // Prepare notifications for status changes (pending, rescheduled, canceled)
        const statusChangeNotifications = statusChangedAppointments.map(appointment => {
            let message;
            if (appointment.status === 'pending') {
                message = `New appointment for ${appointment.patient.name} is pending confirmation.`;
            } else if (appointment.status === 'rescheduled') {
                message = `Appointment for ${appointment.patient.name} has been rescheduled.`;
            } else if (appointment.status === 'canceled') {
                message = `Appointment for ${appointment.patient.name} has been canceled.`;
            }

            return {
                appointmentId: appointment._id,
                patientId: appointment.patient._id,
                message,
                date: appointment.date,
                time: appointment.time,
                description: appointment.description,
                status: appointment.status
            };
        });

        // Combine both types of notifications
        const notifications = [...upcomingNotifications, ...statusChangeNotifications];

        // Count the number of notifications
        const notificationCount = notifications.length;

        if (!notificationCount) {
            return res.status(404).json({ message: 'No notifications found for staff' });
        }

        res.status(200).json({ count: notificationCount, notifications });
    } catch (error) {
        console.error('Error fetching staff notifications:', error);
        res.status(500).json({ message: 'Server error' });
    }
};



// Get confirmed appointments for the logged-in patient
const getConfirmedAppointmentsForPatient = async (req, res) => {
    try {
        const appointmentsRef = admin.database().ref('appointments'); // Reference to the appointments node
        const appointmentsSnapshot = await appointmentsRef
            .orderByChild('userId') // Query by the user's ID
            .equalTo(req.user.id) // Logged-in patient's ID
            .once('value'); // Get the value

        const appointments = [];
        appointmentsSnapshot.forEach(doc => {
            const appointment = doc.val();
            if (['pending', 'approved'].includes(appointment.status)) { // Only pending and approved appointments
                appointments.push({
                    id: doc.key, // Firebase key as the ID
                    ...appointment
                });
            }
        });

        if (!appointments.length) {
            return res.status(404).json({ message: 'No confirmed appointments found for this patient' });
        }

        res.status(200).json(appointments); // Send the response including the appointment ID
    } catch (error) {
        console.error('Error fetching confirmed appointments for patient:', error);
        res.status(500).json({ message: 'Server error' });
    }
};

// Get all appointments for the logged-in patient
const getAllAppointmentsForPatient = async (req, res) => {
    try {
        const appointmentsRef = admin.database().ref('appointments'); // Reference to the appointments node
        const appointmentsSnapshot = await appointmentsRef
            .orderByChild('userId') // Query by the user's ID
            .equalTo(req.user.id) // Logged-in patient's ID
            .once('value'); // Get the value

        const appointments = [];
        appointmentsSnapshot.forEach(doc => {
            appointments.push({
                id: doc.key, // Firebase key as the ID
                ...doc.val()
            });
        });

        if (!appointments.length) {
            return res.status(404).json({ message: 'No appointments found for this patient' });
        }

        res.status(200).json(appointments); // Send the response including the appointment ID
    } catch (error) {
        console.error('Error fetching all appointments for patient:', error);
        res.status(500).json({ message: 'Server error' });
    }
};

// View all appointments for all patients regardless of status
const getAllAppointments = async (req, res) => {
    try {
        const appointmentsRef = admin.database().ref('appointments'); // Reference to the appointments node
        const appointmentsSnapshot = await appointmentsRef.once('value'); // Get all appointments

        const appointments = [];
        appointmentsSnapshot.forEach(doc => {
            appointments.push({
                id: doc.key, // Firebase key as the ID
                ...doc.val()
            });
        });

        if (!appointments.length) {
            return res.status(404).json({ message: 'No appointments found' });
        }

        res.status(200).json(appointments);
    } catch (error) {
        console.error('Error fetching all appointments:', error);
        res.status(500).json({ message: 'Server error' });
    }
};

// Get confirmed appointments
const getConfirmedAppointments = async (req, res) => {
    try {
        const appointmentsRef = admin.database().ref('appointments'); // Reference to the appointments node
        const appointmentsSnapshot = await appointmentsRef
            .orderByChild('status') // Query by status
            .equalTo('approved') // Only confirmed appointments
            .once('value'); // Get the value

        const appointments = [];
        appointmentsSnapshot.forEach(doc => {
            appointments.push({
                id: doc.key, // Firebase key as the ID
                ...doc.val()
            });
        });

        if (!appointments.length) {
            return res.status(404).json({ message: 'No confirmed appointments found' });
        }

        res.status(200).json(appointments); // Send the response including the appointment ID
    } catch (error) {
        console.error('Error fetching confirmed appointments:', error);
        res.status(500).json({ message: 'Server error' });
    }
};

module.exports = {
    bookAppointment,
    rescheduleAppointment,
    cancelAppointment,
    approveAppointment,
    getPatientNotifications,
    getStaffNotifications,
    getConfirmedAppointmentsForPatient,
    getAllAppointmentsForPatient,
    getAllAppointments,
    getConfirmedAppointments,
};