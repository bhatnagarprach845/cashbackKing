import React, { useState, useEffect } from 'react';
import axios from 'axios';

const AdminDashboard = () => {
    const [wallets, setWallets] = useState([]);
    const [payouts, setPayouts] = useState([]);
    const [selectedUser, setSelectedUser] = useState(null);
    const [viewMode, setViewMode] = useState('list');
    const [userReceipts, setUserReceipts] = useState([]);
    const [userDetails, setUserDetails] = useState(null);
    const [selectedReceipt, setSelectedReceipt] = useState(null);

    // NEW STATES FOR ANIMATION
    const [animatingBalance, setAnimatingBalance] = useState(null);

    // Initial load: Fetch Wallets and Payouts
    const fetchWallets = () => {
        axios.get("http://localhost:8080/api/v1/admin/wallets")
            .then(res => setWallets(res.data))
            .catch(err => console.error("Admin access denied", err));
    };

    useEffect(() => {
        fetchWallets();

        axios.get("http://localhost:8080/api/v1/admin/payouts")
            .then(res => setPayouts(res.data))
            .catch(err => console.error("Payout fetch failed", err));
    }, []);

    // Secondary load: Fetch Drill-down data
    useEffect(() => {
        if (!selectedUser) return;

        if (viewMode === 'profile') {
            axios.get(`http://localhost:8080/api/v1/admin/users/${selectedUser.id}/profile`)
                .then(res => setUserDetails(res.data))
                .catch(err => console.error("Profile fetch failed", err));
        } else if (viewMode === 'receipts') {
            axios.get(`http://localhost:8080/api/v1/admin/users/${selectedUser.id}/receipts`)
                .then(res => {
                    const data = Array.isArray(res.data) ? res.data : [];
                    setUserReceipts(data);
                })
                .catch(err => {
                    console.error("Receipts fetch failed", err);
                    setUserReceipts([]);
                });
        }
    }, [viewMode, selectedUser]);

    // Unified Animation Function
    const animateValue = (start, end, duration, setter) => {
        let startTimestamp = null;
        const step = (timestamp) => {
            if (!startTimestamp) startTimestamp = timestamp;
            const progress = Math.min((timestamp - startTimestamp) / duration, 1);
            const current = Math.floor(progress * (end - start) + start);
            setter(current);
            if (progress < 1) {
                window.requestAnimationFrame(step);
            }
        };
        window.requestAnimationFrame(step);
    };

    const handleUserClick = (userId, fullName) => {
        setSelectedUser({ id: userId, name: fullName });
        setViewMode('profile');
    };

    const handlePayout = async (userId, amount) => {
        if (!window.confirm(`Are you sure you want to pay ₹${amount} to this user?`)) return;

        try {
            const res = await axios.post(`http://localhost:8080/api/v1/admin/payouts/initiate/${userId}`, {
                amount: amount
            });

            console.log("Payout initiated:", res.data);

            // Start the countdown animation from current amount to 0 over 1 second
            animateValue(amount, 0, 1000, setAnimatingBalance);

            // 2. IMMEDIATE STATE UPDATE:
                    // Force the local wallets state to 0 so it doesn't "jump back"
                    setWallets(prevWallets => prevWallets.map(w =>
                        w.userId === userId ? { ...w, currentBalance: 0 } : w
                    ));
            alert("Success! Payout ID: " + (res.data.id || res.data));

            // Refresh the actual database values after the animation finishes
            setTimeout(() => {
                fetchWallets();
                setAnimatingBalance(null);
            }, 1200);

        } catch (err) {
            console.error("Payout failed", err);
            alert("Error: " + (err.response?.data || "Could not reach server"));
        }
    };

    const handleBack = () => {
        setSelectedUser(null);
        setViewMode('list');
        setUserDetails(null);
        setUserReceipts([]);
    };

    const downloadReceiptItems = (receipt) => {
        let csvContent = "Description,Quantity,Unit Price,Total Price\n";
        receipt.items.forEach(item => {
            csvContent += `"${item.description}",${item.quantity},${item.unitPrice},${item.totalPrice}\n`;
        });

        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.setAttribute("href", url);
        link.setAttribute("download", `items_${receipt.merchantName}_${receipt.id}.csv`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    };

    const downloadReport = () => {
        window.location.href = "http://localhost:8080/api/v1/admin/wallets/export";
    };

    return (
        <div style={styles.adminContainer}>
            <h2 style={{ color: '#28a745' }}>System Overview (Admin)</h2>

            {viewMode !== 'list' && selectedUser && (
                <div style={styles.detailView}>
                    <button onClick={handleBack} style={styles.backBtn}>← Back to Overview</button>
                    <h3 style={{ color: '#fff' }}>Managing User: {selectedUser.name}</h3>

                    <div style={styles.tabGroup}>
                        <button onClick={() => setViewMode('profile')} style={viewMode === 'profile' ? styles.activeTab : styles.tab}>User Profile</button>
                        <button onClick={() => setViewMode('receipts')} style={viewMode === 'receipts' ? styles.activeTab : styles.tab}>Receipt History</button>
                    </div>

                    {viewMode === 'profile' && userDetails && (
                        <div style={styles.contentBox}>
                            <p><strong>Full Name:</strong> {userDetails.name}</p>
                            <p><strong>Email:</strong> {userDetails.email}</p>
                            <p><strong>UPI ID:</strong> {userDetails.upiId}</p>
                            <p><strong>Cognito ID:</strong> {userDetails.cognitoId}</p>

                            {(() => {
                                const userWallet = wallets.find(w => String(w.userId) === String(selectedUser.id));
                                const balance = userWallet ? userWallet.currentBalance : 0;

                                return (
                                    <>
                                        <p><strong>Current Wallet Balance:</strong>
                                            <span style={{
                                                color: animatingBalance !== null ? '#dc3545' : '#28a745',
                                                fontWeight: 'bold',
                                                fontSize: '1.2em',
                                                marginLeft: '10px'
                                            }}>
                                                ₹{animatingBalance !== null ? animatingBalance : balance.toFixed(2)}
                                            </span>
                                        </p>
                                        <p><strong>Razorpay Fund Account:</strong> {userDetails.razorpayFundAccountId || 'Not Created'}</p>

                                        <button
                                            onClick={() => handlePayout(selectedUser.id, balance)}
                                            style={{
                                                ...styles.payoutBtn,
                                                width: '100%',
                                                marginTop: '20px',
                                                padding: '15px',
                                                backgroundColor: balance > 0 ? '#28a745' : '#555',
                                                cursor: balance > 0 ? 'pointer' : 'not-allowed'
                                            }}
                                            disabled={balance <= 0}
                                        >
                                            💸 Initiate Razorpay Payout (₹{balance.toFixed(2)})
                                        </button>
                                    </>
                                );
                            })()}
                        </div>
                    )}

                    {viewMode === 'receipts' && (
                        <div style={styles.contentBox}>
                            {Array.isArray(userReceipts) && userReceipts.length > 0 ? (
                                <table style={styles.table}>
                                    <thead>
                                        <tr style={styles.headerRow}>
                                            <th>Merchant</th>
                                            <th>Amount</th>
                                            <th>Status</th>
                                            <th>Date</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {userReceipts.map((r, index) => (
                                            <tr key={r.id || index} style={styles.row}>
                                                <td>
                                                    <button onClick={() => setSelectedReceipt(r)} style={styles.linkButton}>
                                                        {r.merchantName || 'Unknown Merchant'}
                                                    </button>
                                                </td>
                                                <td>₹{r.totalAmount?.toFixed(2)}</td>
                                                <td><span style={styles.statusBadge}>{r.status}</span></td>
                                                <td>{r.purchaseDate || 'N/A'}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            ) : <p>No receipts uploaded yet.</p>}
                        </div>
                    )}
                </div>
            )}

            {/* RECEIPT ITEMS MODAL */}
            {selectedReceipt && (
                <div style={styles.modalOverlay}>
                    <div style={styles.modalContent}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #ddd', paddingBottom: '10px' }}>
                            <h3 style={{ color: '#333', margin: 0 }}>Items for {selectedReceipt.merchantName}</h3>
                            <button onClick={() => setSelectedReceipt(null)} style={{ border: 'none', background: 'none', cursor: 'pointer', fontSize: '18px' }}>✖</button>
                        </div>
                        <div style={{ maxHeight: '300px', overflowY: 'auto', marginTop: '15px' }}>
                            <table style={{ width: '100%', borderCollapse: 'collapse', color: '#333' }}>
                                <thead>
                                    <tr style={{ textAlign: 'left', borderBottom: '2px solid #eee' }}>
                                        <th>Description</th>
                                        <th>Qty</th>
                                        <th>Price</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {selectedReceipt.items?.map(item => (
                                        <tr key={item.id} style={{ borderBottom: '1px solid #eee' }}>
                                            <td style={{ padding: '8px 0' }}>{item.description}</td>
                                            <td>{item.quantity}</td>
                                            <td>₹{item.totalPrice?.toFixed(2)}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                        <button onClick={() => downloadReceiptItems(selectedReceipt)} style={styles.downloadBtn}>
                            Download Items as CSV
                        </button>
                    </div>
                </div>
            )}

            {/* MAIN LIST VIEW */}
            {viewMode === 'list' && (
                <>
                    <div style={styles.buttonGroup}>
                        <button onClick={downloadReport} style={styles.downloadBtn}>Download Wallet Report</button>
                        <button onClick={() => window.open("http://localhost:8080/api/v1/admin/payouts/report")} style={styles.payoutBtn}>Download Payout CSV</button>
                    </div>

                    <h3 style={styles.subTitle}>Active Wallets</h3>
                    <table style={styles.table}>
                        <thead>
                            <tr style={styles.headerRow}>
                                <th>User ID / Name</th>
                                <th>UPI ID</th>
                                <th>Current Balance</th>
                                <th>Last Updated</th>
                            </tr>
                        </thead>
                        <tbody>
                            {wallets.map(wallet => (
                                <tr key={wallet.userId} style={styles.row}>
                                    <td>
                                        <button onClick={() => handleUserClick(wallet.userId, wallet.fullName)} style={styles.linkButton}>
                                            {wallet.fullName || 'Unknown User'}
                                        </button>
                                        <br /><small style={{ color: '#888' }}>{wallet.userId}</small>
                                    </td>
                                    <td>{wallet.upiId || 'N/A'}</td>
                                    <td>₹{wallet.currentBalance?.toFixed(2)}</td>
                                    <td>{wallet.lastUpdated?.split('T')[0]}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>

                    <h3 style={styles.subTitle}>Recent Redemptions (Payouts)</h3>
                    <table style={styles.table}>
                        <thead>
                            <tr style={styles.headerRow}>
                                <th>User ID / Name</th>
                                <th>Amount Paid</th>
                                <th>Date</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            {payouts.map((tx, index) => (
                                <tr key={index} style={styles.row}>
                                    <td>
                                        <strong>{tx.userName}</strong>
                                        <br/>
                                        <small style={{ color: '#888', fontSize: '10px' }}>{tx.userId}</small>
                                    </td>
                                    <td style={{ color: '#dc3545', fontWeight: 'bold' }}>-₹{tx.amountAwarded}</td>
                                    <td>{tx.processedAt?.split('T')[0]}</td>
                                    <td><span style={styles.statusBadge}>{tx.status}</span></td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </>
            )}
        </div>
    );
};

// ... keep your existing styles object ...
const styles = {
    adminContainer: { marginTop: '40px', padding: '30px', backgroundColor: '#1a1a1a', borderRadius: '12px', minHeight: '80vh' },
    subTitle: { color: '#ccc', borderBottom: '1px solid #444', paddingBottom: '10px', marginTop: '30px' },
    buttonGroup: { display: 'flex', gap: '15px', marginBottom: '20px' },
    table: { width: '100%', borderCollapse: 'collapse', color: 'white', marginBottom: '20px' },
    headerRow: { backgroundColor: '#333', textAlign: 'left' },
    row: { borderBottom: '1px solid #333' },
    downloadBtn: { backgroundColor: '#007bff', color: 'white', border: 'none', padding: '10px 15px', borderRadius: '6px', cursor: 'pointer', fontWeight: 'bold'},
    payoutBtn: { backgroundColor: '#28a745', color: 'white', border: 'none', padding: '10px 15px', borderRadius: '6px', cursor: 'pointer', fontWeight: 'bold' },
    statusBadge: { backgroundColor: '#444', padding: '2px 8px', borderRadius: '4px', fontSize: '11px', color: '#ffc107' },
    linkButton: { background: 'none', border: 'none', color: '#28a745', textDecoration: 'underline', cursor: 'pointer', padding: 0, fontSize: '14px', fontWeight: 'bold' },
    detailView: { backgroundColor: '#222', padding: '20px', borderRadius: '8px', marginTop: '20px' },
    backBtn: { backgroundColor: '#666', color: 'white', border: 'none', padding: '5px 10px', borderRadius: '4px', cursor: 'pointer', marginBottom: '15px' },
    tabGroup: { display: 'flex', gap: '10px', marginBottom: '20px' },
    tab: { backgroundColor: '#333', color: '#ccc', border: 'none', padding: '10px 20px', cursor: 'pointer' },
    activeTab: { backgroundColor: '#28a745', color: 'white', border: 'none', padding: '10px 20px', fontWeight: 'bold' },
    contentBox: { padding: '20px', border: '1px solid #444', color: '#eee' },
    modalOverlay: { position: 'fixed', top: 0, left: 0, width: '100%', height: '100%', backgroundColor: 'rgba(0,0,0,0.8)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000 },
    modalContent: { backgroundColor: 'white', padding: '25px', borderRadius: '12px', width: '90%', maxWidth: '500px', textAlign: 'left' }
};

export default AdminDashboard;