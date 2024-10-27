## DENTAL DIARY

## Purpose of the App
Dental Diary is a comprehensive platform designed for dentists and patients (clients) to efficiently manage dental appointments and provide convenient access to dental services and educational resources. The app's core focus is to simplify the scheduling process, allow seamless communication between dentists and clients, and offer support through educational materials on oral health.

For Clients (Patients):
Dental Diary allows clients to:

Register and Log In using personal details like name, email, phone number, and password. Clients can also log in using Single Sign-On (SSO), such as Google, or use fingerprint recognition for faster access.
Search for Dentists using a variety of filters such as location, services, and availability. Clients can see dentist details like address, descriptions, and services offered in either a list or map view, making it easier to find the best-suited dentist near them.
Book and Manage Appointments directly from the app. Clients can select a date, view available time slots, and describe the reason for their appointment. They can also view their appointments in a calendar format.
Receive Real-time Notifications to stay updated on appointment reminders, rescheduling, or cancellations.
Navigate to Appointments using integrated directions within the app’s map feature, guiding clients to the dentist's office.
Access the Health Zone, where they can find educational dental health videos and explanations of frequently asked questions, helping them better understand oral hygiene and common dental concerns.
Manage Account Settings: Clients can change their personal details and update preferences through the app’s user-friendly settings page.
Password Management: Clients can reveal passwords using an eye icon, retrieve forgotten passwords, and reset them by validating their username and email.

For Dentists:
Dental Diary offers a streamlined solution for dental practices to manage appointments and interact with patients effectively:

Register and Log In using practice information such as name, phone number, email, and address. Dentists can also log in using SSO and fingerprint recognition.
Manage Availability: Dentists can book time off, either for specific days or time slots, blocking them from patient bookings. This ensures their availability calendar is always up to date.
View and Manage Client Appointments: Dentists can view a list of all booked appointments, approve or reschedule appointments as needed. They can also view a calendar of appointments to see all scheduled bookings on a specific day.
Receive Real-time Notifications for all client bookings, reschedules, and cancellations, allowing them to keep their schedule organized.
Additional Features:
Push Notifications: Both clients and dentists receive real-time notifications and reminders about upcoming appointments, cancellations, and other important updates.
Offline Functionality: The app allows users to book and manage appointments offline, with changes automatically synced to the cloud database once the user is back online.
User-Friendly Interface: The app provides a simple and intuitive home page that allows all users to easily navigate features like booking, appointment management, and viewing educational resources.
Database and API Integration:
All user data, including appointments and notifications, is stored in a secure online database. The REST APIs ensure smooth communication between the app and the database for real-time updates, even when offline access is utilized.

## Design Considerations
The design of Dental Diary was guided by several key considerations to ensure the app is user-friendly, secure, and accessible for both dentists and clients (patients). These considerations were aimed at creating an efficient, intuitive, and reliable experience for all users while maintaining modern best practices for app development.

1. User Experience (UX) and Interface Design
Simplicity and Usability: The interface was designed to be clean and intuitive, catering to a wide range of users, including those who may not be tech-savvy. The app provides easy navigation through a clear home page, which displays options for appointment booking, searching dentists, and accessing account settings.
Efficient Navigation: We ensured that both dentists and clients can easily access the core features of the app without unnecessary complexity. For instance, clients can quickly search for nearby dentists, book appointments, and receive directions.
Visual Feedback: Throughout the app, users receive clear feedback when actions are taken, such as when an appointment is booked, or settings are updated. Visual indicators like loading bars and success messages ensure users understand when tasks are completed.
Accessibility: Features like the eye icon for revealing passwords and the ability to use voice navigation in the map ensure that the app is accessible for a variety of users, including those with visual impairments.
2. Voice Navigation for Maps
Hands-Free Navigation: For safety and convenience, the map feature of the app uses voice navigation to guide users to their dental appointments. This allows patients driving to a clinic to receive turn-by-turn directions without needing to look at their phones, reducing distractions and increasing safety while traveling.
3. Security
User Authentication: Dental Diary supports Single Sign-On (SSO) using Google, as well as fingerprint authentication, ensuring secure and easy access for users. These methods provide a high level of security, making login both convenient and safe.
Password Management: The app includes password visibility options using an eye icon and offers secure password reset functionality. Forgotten passwords can be retrieved through email validation, providing a secure way to manage access.
Data Encryption: Sensitive data, such as user information and appointment details, are stored securely in an encrypted database. Additionally, all data transferred between the app and server is encrypted to ensure the privacy and protection of user data.
4. Offline Functionality
Seamless Offline Access: Recognizing that users might not always have access to a stable internet connection, Dental Diary allows dentists and clients to manage their appointments even while offline. Any updates made while offline will automatically sync with the cloud database when the app returns online, providing continuous access without disruption.
5. Push Notifications and Real-time Updates
Appointment Reminders and Updates: Push notifications were integrated into the design to ensure users are always up to date on their appointments. This includes reminders for upcoming appointments, rescheduling notifications, and cancellations. Real-time notifications keep users informed, helping them manage their schedules effectively.
6. Calendar and Appointment Management
Clear Appointment Visibility: The calendar view was designed to make it easy for both clients and dentists to see upcoming appointments at a glance. For dentists, this view is crucial for efficiently managing daily schedules, while for clients, it ensures they don’t miss appointments.
Flexible Rescheduling: Both clients and dentists can easily reschedule appointments through the app, ensuring flexibility and making appointment management less stressful.
7. Health Zone for Educational Resources
Providing Value Beyond Appointments: The inclusion of a Health Zone with educational videos and articles was driven by the desire to offer users more than just appointment management. By addressing frequently asked dental health questions, the app empowers clients to make informed decisions about their dental care, adding educational value to the overall experience.
8. Performance and Scalability
Optimized for Performance: The app was designed with performance in mind, ensuring that all operations, including appointment booking, notifications, and map navigation, are handled smoothly even on devices with lower processing power. Efficient use of network resources ensures that the app remains responsive, even in areas with limited connectivity.
Scalable Architecture: Given the potential for growth in both user base and feature set, the app was built with scalability in mind. This allows it to handle a growing number of users, dentists, and appointments without sacrificing performance.
9. API and Database Integration
Cloud-Based Data Management: All user data, including appointments, user profiles, and notifications, are stored securely in a cloud database. This ensures that users have access to their data from any device and allows for real-time updates across multiple platforms. The use of REST APIs ensures seamless communication between the app and the backend systems.
These design considerations aim to deliver a user-friendly, secure, and efficient solution that meets the diverse needs of both dentists and patients. By incorporating modern technologies like voice navigation, offline functionality, and secure authentication methods, Dental Diary stands out as an all-in-one tool for dental appointment management and patient education.

## GitHub Utilisation
Throughout the development of the Dental Diary app, my team and I followed a well-structured version control process using Git and GitHub. This ensured that the project was efficiently managed, code was properly reviewed, and all team members were able to collaborate seamlessly.

1. Branching Strategy
Feature-based Branching: For every new feature that we implemented, a new branch was created. This allowed us to work on separate features in isolation without affecting the main codebase. Each team member would create a dedicated branch for their feature or task.
For example, if a team member was working on appointment booking, they would create a branch named feature/appointment-booking.
This strategy reduced the chances of breaking the main branch and ensured that each feature was tested and reviewed before integration.
New Branches Using Android Studio VCS: Using Android Studio’s built-in Version Control System (VCS), we easily created new branches and switched between them without needing to use the command line. This VCS interface allowed us to directly integrate Git actions such as committing, pushing, and creating branches while working within the Android Studio environment.
2. Committing and Pushing Code
We regularly committed our progress to ensure that changes were saved and tracked incrementally. Each commit message followed a clear and consistent format, providing a short description of the changes made.
Commit Example: Added appointment booking logic and validation
Once changes were committed, they were pushed to the remote repository on GitHub, again through Android Studio’s VCS. This integration allowed us to efficiently push our branches to GitHub without needing to exit our development environment.
3. Pull Requests and Code Review
Creating Pull Requests: Once a feature was completed on a branch, a pull request (PR) was created on GitHub. The PR would request to merge the feature branch into the main branch (main) or a development branch, depending on the workflow.
The pull request provided a space for team members to review the code, leave comments, and suggest improvements. This process ensured that code was thoroughly reviewed before being merged into the main branch.
Code Review and Approval: Team members reviewed each other’s code using GitHub’s pull request review features. Once the PR was reviewed and any requested changes were made, the pull request would be approved and ready for merging.
4. Handling Merge Conflicts
Resolving Merge Conflicts: Occasionally, when two branches were being merged, merge conflicts arose. These conflicts occurred when changes were made to the same parts of the code in different branches. In such cases, we resolved the conflicts using GitHub’s conflict resolution tools.
This involved reviewing the conflicting code and manually deciding which version of the code should be kept or merged. GitHub’s interface provided an easy way to compare changes and resolve conflicts effectively.
After resolving conflicts on GitHub, the changes were merged into the main branch.
5. Merging and Pulling Changes
Once the pull request was approved and any conflicts were resolved, the feature branch was merged into the main branch. This was done directly on GitHub after the successful code review.
After the merge, all team members would pull the latest changes from the main branch back into their local environments using Android Studio’s VCS. This ensured that everyone was working with the most up-to-date codebase.
6. Automated Testing

This process of branching, committing, pushing, and pull requests helped maintain a structured workflow and ensured that our team could work on the project collaboratively without disrupting each other's progress. By leveraging both Android Studio’s VCS integration and GitHub’s powerful code review and conflict resolution features, we were able to maintain a smooth development cycle, keep the codebase clean, and track changes efficiently.

Youtube link: https://youtu.be/f5jRrUuLCeE

##Screenshots
Dentist Features:
Book Appointment -
![Screenshot of My Application](https://github.com/user-attachments/assets/bffd9531-3d67-4502-886a-2cd8adf09c08)
<img src="[UR](https://github.com/user-attachments/assets/bffd9531-3d67-4502-886a-2cd8adf09c08)" alt="Alt text" width="500"/>


Settings -
![settings - dentist](https://github.com/user-attachments/assets/ecf8a254-600e-4880-8cec-10b03dded92a)


Client Features:
Book Appointment -
![book App client](https://github.com/user-attachments/assets/c89f6d27-e33d-440f-a5d3-7431806d60af)
Settings - 
![settings - client](https://github.com/user-attachments/assets/2838137c-03a3-4f71-a199-4d1a8b24174d)
Maps -
![maps - client ](https://github.com/user-attachments/assets/1731fd5b-561b-4b1d-bd0c-07b8a3d2a386)
Helathzone -
![healthzone](https://github.com/user-attachments/assets/49cdad5b-f59b-4d4f-9906-330d1f2b81f6)






