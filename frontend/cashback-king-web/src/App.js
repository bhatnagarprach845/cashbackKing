import React, { useState, useEffect } from 'react'; // 1. Added useEffect
import { fetchAuthSession } from 'aws-amplify/auth'; // 2. Added to get the token
import { syncUserWithBackend } from './api/cashbackApi';
import { BrowserRouter as Router, Routes, Route, Link, Navigate } from 'react-router-dom'
import { Authenticator } from '@aws-amplify/ui-react';
import { Amplify } from 'aws-amplify'; // Import Amplify
import '@aws-amplify/ui-react/styles.css'; // Don't forget the styles!
import Dashboard from './Dashboard';
import FileUpload from './FileUpload';
import AdminDashboard from './AdminDashboard';

// 1. Configure Amplify outside the component
Amplify.configure({
  Auth: {
    Cognito: {
      userPoolId: 'us-east-2_OfWs7erUH', // Your User Pool ID from AWS
      userPoolClientId: '5mm6akdvcc4skv9m2anfgmb1ej', // Your App Client ID
      loginWith: {
              username: true,
              email: true
            }
    }
  }
});

function App() {
  // These need to stay here so both routes can stay in sync if needed
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const handleUploadSuccess = () => {
    setRefreshTrigger(prev => prev + 1);
  };

// --- NEW SYNC LOGIC ---
  const SyncWrapper = ({ user }) => {
    useEffect(() => {
      const performSync = async () => {
        try {
          // Get the JWT Token from the current session
          const session = await fetchAuthSession();
          const token = session.tokens.accessToken.toString();

          // Call your Spring Boot backend
          await syncUserWithBackend(token);
          console.log("Prachi :: User synced with Supabase successfully");
        } catch (err) {
          console.error("Prachi :: Auth session error", err);
        }
      };

      if (user) performSync();
    }, [user]);

    return null; // This component doesn't render anything, just handles logic
  };

  return (
    <Router>
      <Authenticator signUpAttributes={['email']}>
        {({ signOut, user }) => (
          <div className="App" style={styles.appContainer}>
          {/* 4. Trigger the sync when the user object is available */}
            <SyncWrapper user={user} />
            <nav style={styles.nav}>
              <h2 style={{ color: '#28a745', margin: 0 }}>Cashback King</h2>
              <div style={styles.navLinks}>
                <Link to="/" style={styles.link}>My Rewards</Link>

                {/* Secure Check: Only show Admin link for you */}
                {user.username === 'prachi' && (
                  <Link to="/admin" style={styles.adminLink}>Admin Panel</Link>
               )
               }

                <button
                  onClick={signOut}
                  style={styles.logoutBtn}
                >
                  Sign Out
                </button>
              </div>
            </nav>

            <Routes>
              <Route path="/" element={
                <main style={{ padding: '20px' }}>
                  <Dashboard refreshTrigger={refreshTrigger} username={user.username}/>
                  <div style={{ margin: '30px auto', maxWidth: '400px', borderTop: '1px solid #444' }}></div>
                  <FileUpload onUploadSuccess={handleUploadSuccess} />
                </main>
              } />

              {/* FIXED: Added a Guard to the Admin Route */}
                <Route
                  path="/admin"
                  element={
                    user.username === 'prachi' ? (
                      <AdminDashboard />
                    ) : (
                      <Navigate to="/" replace /> // Redirect to Home if not admin
                    )
                  }
                />
            </Routes>
          </div>
        )}
      </Authenticator>
    </Router>
  );
}

const styles = {
  appContainer: {
    backgroundColor: '#1a1a1a',
    minHeight: '100vh',
    color: 'white'
  },
  nav: {
    display: 'flex',
    justifyContent: 'space-between',
    padding: '20px',
    borderBottom: '1px solid #333',
    alignItems: 'center'
  },
  navLinks: { display: 'flex', gap: '20px', alignItems: 'center' },
  link: { color: 'white', textDecoration: 'none' },
  adminLink: { color: '#ffc107', fontWeight: 'bold', textDecoration: 'none' },
  logoutBtn: {
    backgroundColor: 'transparent',
    color: '#888',
    border: '1px solid #444',
    padding: '5px 10px',
    borderRadius: '4px',
    cursor: 'pointer'
  }
};

export default App;