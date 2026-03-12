import React, { useState, useEffect } from 'react';
import axios from 'axios';

const AdminDashboard = () => {
    const [wallets, setWallets] = useState([]);
    const downloadReport = () => {
        // This will trigger the browser's download manager
        window.location.href = "http://localhost:8080/api/v1/admin/wallets/export";
    };

    useEffect(() => {
        axios.get("http://localhost:8080/api/v1/admin/wallets")
            .then(res => setWallets(res.data))
            .catch(err => console.error("Admin access denied", err));
    }, []);

    return (
        <div style={styles.adminContainer}>
            <h2 style={{ color: '#28a745' }}>System Overview (Admin)</h2>
            <table style={styles.table}>
                <thead>
                    <tr>
                        <th>User ID</th>
                        <th>Current Balance</th>
                        <th>Total Payouts</th>
                    </tr>
                </thead>
                <tbody>
                    {wallets.map(wallet => (
                        <tr key={wallet.userId}>
                            <td>{wallet.userId}</td>
                            <td>₹{wallet.currentBalance.toFixed(2)}</td>
                            <td>{wallet.payoutCount || 0}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
            <button
                onClick={downloadReport}
                style={styles.downloadBtn}
            >
                Download Excel Report
            </button>
        </div>
    );
};

const styles = {
    adminContainer: { marginTop: '40px', padding: '20px', backgroundColor: '#222', borderRadius: '12px' },
    table: { width: '100%', borderCollapse: 'collapse', color: 'white' },
    // Add more table styling here
};

export default AdminDashboard;