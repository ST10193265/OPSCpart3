const admin = require('../config/db.js');

// Book time off for a dentist
const bookTimeOff = async (req, res) => {
    const { startDate, endDate, reason } = req.body;
    const dentistId = req.user.id; // Get dentistId from the authenticated user

    try {
        // Generate a new time-off ID
        const timeOffId = admin.database().ref('timeoff').push().key;
        
        const timeOff = {
            timeOffId,
            startDate,
            endDate,
            reason,
            dentistId,
            status: 'pending', // Initial status
            createdAt: admin.database.ServerValue.TIMESTAMP,
        };

        // Check for existing appointments in the time-off period
        const appointmentsSnapshot = await admin.database()
            .ref('appointments')
            .orderByChild('dentistId')
            .equalTo(dentistId)
            .once('value');

        const conflictingAppointments = [];
        appointmentsSnapshot.forEach(doc => {
            const appointment = doc.val();
            if (appointment.date >= startDate && appointment.date <= endDate) {
                conflictingAppointments.push(appointment);
            }
        });

        if (conflictingAppointments.length > 0) {
            return res.status(400).json({
                message: 'Cannot book time off - you have existing appointments during this period',
                conflicts: conflictingAppointments
            });
        }

        // Set the time-off data in the database
        await admin.database().ref(`timeoff/${timeOffId}`).set(timeOff);
        
        res.status(201).json({
            message: 'Time off booked successfully',
            timeOffId,
        });
    } catch (error) {
        console.error("Error booking time off:", error);
        res.status(500).json({ message: 'Server error' });
    }
};

// Get all time-off records for a dentist
const getDentistTimeOff = async (req, res) => {
    const dentistId = req.user.id;

    try {
        const timeOffSnapshot = await admin.database()
            .ref('timeoff')
            .orderByChild('dentistId')
            .equalTo(dentistId)
            .once('value');

        const timeOffRecords = [];
        timeOffSnapshot.forEach(doc => {
            timeOffRecords.push({
                id: doc.key,
                ...doc.val()
            });
        });

        if (!timeOffRecords.length) {
            return res.status(404).json({ message: 'No time-off records found' });
        }

        res.status(200).json(timeOffRecords);
    } catch (error) {
        console.error("Error fetching time-off records:", error);
        res.status(500).json({ message: 'Server error' });
    }
};

// Cancel time off
const cancelTimeOff = async (req, res) => {
    const timeOffId = req.params.timeOffId;
    const dentistId = req.user.id;

    try {
        const timeOffRef = admin.database().ref(`timeoff/${timeOffId}`);
        const timeOffSnapshot = await timeOffRef.once('value');

        if (!timeOffSnapshot.exists()) {
            return res.status(404).json({ message: 'Time-off record not found' });
        }

        const timeOff = timeOffSnapshot.val();

        if (timeOff.dentistId !== dentistId) {
            return res.status(403).json({ message: 'Unauthorized to cancel this time-off' });
        }

        await timeOffRef.update({
            status: 'cancelled',
            updatedAt: admin.database.ServerValue.TIMESTAMP,
        });

        res.status(200).json({ message: 'Time-off cancelled successfully' });
    } catch (error) {
        console.error("Error cancelling time-off:", error);
        res.status(500).json({ message: 'Server error' });
    }
};

module.exports = {
    bookTimeOff,
    getDentistTimeOff,
    cancelTimeOff,
};