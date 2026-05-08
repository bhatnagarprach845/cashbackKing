const BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

export const syncUserWithBackend = async (idToken) => {
    try {
        const response = await fetch(`${BASE_URL}/users/sync`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${idToken}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error('Failed to sync user');
        }

        return await response.json();
    } catch (error) {
        console.error("Prachi :: Sync Error:", error);
    }
};