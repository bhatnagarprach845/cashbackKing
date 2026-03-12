import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { fetchAuthSession } from 'aws-amplify/auth';

const Dashboard = ({ refreshTrigger }) => {
    const [data, setData] = useState(null);
    const [isRedeeming, setIsRedeeming] = useState(false);

    // Fetch the status from Spring Boot
    const fetchStatus = async () => {
      // Get the current session token from Amplify
        const session = await fetchAuthSession();
        const token = session.getIdToken().getJwtToken();

        try {
          const res = await axios.get("http://localhost:8080/api/v1/receipts/payout-status", {
            headers: { Authorization: `Bearer ${token}` } // Send the token!
          });
          setData(res.data);
        } catch (err) {
          console.error("Error fetching status", err);
        }
      }

    useEffect(() => {
        fetchStatus();
    }, [refreshTrigger]);

    // Function to trigger the ₹30 redemption
    const handleRedeem = async () => {
        setIsRedeeming(true);
        try {
            const res = await axios.post("http://localhost:8080/api/v1/receipts/redeem");
            if (res.status === 200) {
                alert("Success! Payout initiated.");
                fetchStatus(); // Refresh the dashboard to show ₹0 balance
            }
        } catch (err) {
            alert(err.response?.data?.error || "Redemption failed. Please try again.");
        } finally {
            setIsRedeeming(false);
        }
    };

    if (!data) return <p style={{ color: 'white', textAlign: 'center' }}>Loading your rewards...</p>;

    const progressPercent = Math.min((data.currentBalance / data.threshold) * 100, 100);
    const isEligible = data.currentBalance >= data.threshold;

    const getBarColor = () => {
        if (progressPercent < 30) return '#dc3545'; // Red
        if (progressPercent < 70) return '#ffc107'; // Yellow
        return '#28a745'; // Green
    };

    return (
        <div style={styles.card}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <h3 style={{ color: '#333', margin: 0 }}>Your Rewards</h3>
                {/* CLAIM BUTTON */}
                <button
                    onClick={handleRedeem}
                    disabled={!isEligible || isRedeeming}
                    style={{
                        ...styles.redeemBtn,
                        backgroundColor: isEligible ? '#28a745' : '#ccc',
                        cursor: isEligible ? 'pointer' : 'not-allowed'
                    }}
                >
                    {isRedeeming ? '...' : 'Claim'}
                </button>
            </div>

            <p style={styles.balance}>₹{data.currentBalance.toFixed(2)} / ₹{data.threshold}</p>

            <div style={styles.progressBase}>
                <div style={{
                    ...styles.progressBar,
                    width: `${progressPercent}%`,
                    backgroundColor: getBarColor()
                }}></div>
            </div>

            <p style={styles.message}>{data.statusMessage}</p>

            <h4 style={{ color: '#333', marginTop: '20px' }}>Recent Activity</h4>
            <ul style={styles.list}>
                {data.recentTransactions && data.recentTransactions.length > 0 ? (
                    data.recentTransactions.map((tx, index) => (
                        <li key={index} style={styles.listItem}>
                            <div style={{ display: 'flex', flexDirection: 'column' }}>
                                <span style={{ fontSize: '13px' }}>{tx.date.split('T')[0]}</span>
                                <small style={{ color: '#888', fontSize: '10px' }}>{tx.status}</small>
                            </div>
                            <span style={{ fontWeight: 'bold', color: tx.status === 'REDEEMED' ? '#dc3545' : '#28a745' }}>
                                {tx.status === 'REDEEMED' ? '-' : '+'}₹{tx.amount}
                            </span>
                        </li>
                    ))
                ) : (
                    <li style={{ ...styles.listItem, color: '#999', justifyContent: 'center' }}>No transactions yet</li>
                )}
            </ul>
        </div>
    );
};

const styles = {
    card: { padding: '20px', border: '1px solid #ddd', borderRadius: '12px', maxWidth: '400px', margin: '20px auto', backgroundColor: '#fff', boxShadow: '0 4px 6px rgba(0,0,0,0.1)' },
    balance: { fontSize: '24px', fontWeight: 'bold', margin: '10px 0', color: '#28a745' },
    progressBase: { width: '100%', height: '12px', backgroundColor: '#e0e0e0', borderRadius: '6px', overflow: 'hidden', margin: '10px 0' },
    progressBar: { height: '100%', transition: 'width 0.5s ease-in-out' },
    message: { fontSize: '14px', color: '#666', marginTop: '8px' },
    redeemBtn: { border: 'none', color: 'white', padding: '6px 12px', borderRadius: '6px', fontWeight: 'bold', fontSize: '12px', transition: 'background 0.3s' },
    list: { listStyle: 'none', padding: 0, marginTop: '10px', textAlign: 'left', color: '#333' },
    listItem: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', borderBottom: '1px solid #eee' },
};

export default Dashboard;