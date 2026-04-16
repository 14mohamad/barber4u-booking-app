# 💈 Barber4U – Smart Barbershop Booking System

![GitHub repo size](https://img.shields.io/github/repo-size/14mohamad/barber4u-booking-app)
![GitHub last commit](https://img.shields.io/github/last-commit/14mohamad/barber4u-booking-app)

## 📌 Overview
Barber4U is an Android application developed as part of a Software Engineering course.

The system provides a complete digital solution for managing barbershop chains, replacing traditional phone-based booking with a modern, efficient, and user-friendly mobile application.

The application supports multiple user roles, real-time updates, and integrates modern technologies such as Firebase and Google Maps.

**Technologies:** Java, Android, Firebase (Firestore, Authentication, FCM), Google Maps API

---

## 🎯 Problem Statement
Many barbershops still rely on phone calls or walk-ins to manage appointments, which leads to:
- Scheduling conflicts
- Long wait times
- Inefficient management
- Poor customer experience

Barber4U solves this by enabling customers to easily book appointments and allowing barbers and managers to efficiently manage schedules and operations.

---

## 🚀 Key Features

### 👤 Authentication
- User registration and login using Firebase Authentication
- Role-based access (Admin / Barber / Customer)

### 📅 Appointment Booking System
- Select branch → barber → date → time
- Real-time availability checking
- Booking status management (Pending / Approved / Rejected)

### ✂️ Barber Management
- View and manage appointments
- Approve / reject / suggest alternative time
- Upload gallery images (via camera or gallery)

### 🧑‍💼 Admin Dashboard
- Manage branches
- Manage barbers
- View all appointments
- Control system permissions

### 📍 Location & Maps
- Google Maps integration
- View nearby branches
- Navigation support

### 🔔 Notifications & Messaging
- Real-time notifications
- Messaging system between users
- Push notifications using Firebase Cloud Messaging (FCM)

---

## 🧠 System Architecture

The application follows the **MVVM (Model-View-ViewModel)** architecture:

- **Model** → Firebase (Firestore, Authentication)
- **View** → Activities & Fragments (UI)
- **ViewModel** → Business logic & state management
- **Repository** → Data layer abstraction

This structure ensures clean separation of concerns, maintainability, and scalability.

---

## 👥 User Roles

### 🧑 Customer
- Find nearby branches
- View barbers and galleries
- Check availability
- Book appointments
- Rate barbers after service

### ✂️ Barber
- Manage schedule
- Approve/reject appointments
- Suggest alternative times
- Upload work to gallery

### 👑 Admin
- Manage branches and barbers
- View system-wide appointments
- Control permissions and roles

---

## 🔄 Core Workflow

### Appointment Booking Flow
1. Customer selects a branch
2. Selects a barber
3. Chooses date and time
4. Submits booking request
5. Barber approves or rejects

After approval:
- Appointment is saved in database
- Both sides receive updates

---

## 🧪 Testing

### ✅ Functional Testing
- Booking appointments
- Managing schedules
- Rating system

### ✅ Unit Testing
- Role parsing validation
- Adapter logic testing
- Firebase data operations

### ✅ UI Testing (Espresso)
- Booking screen validation
- Date/time selection
- Button state validation

---

## 📊 System Design

The system includes multiple UML diagrams:

- Class Diagram
- Sequence Diagram (booking process)
- Activity Diagram
- ERD (database structure)

---

## 🛠 Technologies Used

- Java (Android)
- Firebase Authentication
- Firebase Firestore
- Firebase Cloud Messaging (FCM)
- Google Maps API
- MVVM Architecture
- RecyclerView & Fragments
- Gradle

---

## 📸 Screenshots

*(Add your app screenshots here)*

---

## 📱 System Modules

- Authentication Module
- Booking Module
- Barber Management Module
- Admin Module
- Messaging Module
- Notification System
- Map & Location Module

---

## ⚠️ Constraints

- Android only (no iOS support)
- Requires internet connection
- Firebase-based backend
- Limited development time (semester project)

---

## 🔮 Future Improvements

- Add payment system
- Add haircut types and pricing
- Improve map features
- Add profile images
- Enhance UI/UX design

---

## 👨‍💻 Team

- Mohamad Mousa – Core system, booking system, barber & customer modules
- Eitan Beriy – Maps API, notifications, messaging system
- Yuval Frankel – Firebase integration

---

## 📦 Repository Structure

app/
├── activities/
├── fragments/
├── adapters/
├── viewmodel/
├── repository/
├── models/
└── data/

---

## 💡 Highlights

- Real-world problem solving
- Complete Android application with real-world functionality and user interaction
- Clean architecture (MVVM)
- Firebase integration
- Multi-role system
- Real-time updates

---

## 📄 License
This project was developed for educational purposes as part of a Software Engineering course.

---

## 👨‍💻 Author
Mohamad Mousa  
GitHub: https://github.com/14mohamad
