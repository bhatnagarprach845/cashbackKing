import React, { useState } from 'react';
import axios from 'axios';

const FileUpload = (props) => {
    const [file, setFile] = useState(null);
    const [preview, setPreview] = useState(null); // New state for image preview
    const [status, setStatus] = useState("Idle");

    const onFileChange = (event) => {
        const selectedFile = event.target.files[0];
        setFile(selectedFile);

        if (selectedFile) {
            // Create a temporary URL for the image
            const objectUrl = URL.createObjectURL(selectedFile);
            setPreview(objectUrl);
        }
    };

    const onUpload = async () => {
        if (!file) return alert("Please select a file first!");

        const formData = new FormData();
        formData.append("file", file);
        setStatus("Uploading...");

       try {
           const response = await axios.post("http://localhost:8080/api/v1/receipts/upload-local", formData, {
               headers: { "Content-Type": "multipart/form-data" }
           });

           setStatus("Success! Reward added.");

           // Notify the parent (App.js) to refresh the dashboard
           if (props.onUploadSuccess) {
               props.onUploadSuccess();
           }
       } catch (error) {
           setStatus("Failed to upload.");
       }
    };

    return (
        <div style={styles.container}>
            <h2>Cashback King</h2>
            <p>Select your bill to earn rewards</p>

            <input type="file" accept="image/*" onChange={onFileChange} style={styles.input} />

            {/* PREVIEW SECTION */}
            {preview && (
                <div style={styles.previewContainer}>
                    <img src={preview} alt="Bill Preview" style={styles.image} />
                </div>
            )}

            <button
                onClick={onUpload}
                disabled={!file || status === "Uploading..."}
                style={styles.button}
            >
                {status === "Uploading..." ? "Processing..." : "Submit Bill"}
            </button>

            <p style={styles.statusText}>{status}</p>
        </div>
    );
};

// Basic Styling
const styles = {
    container: { padding: '40px', textAlign: 'center', fontFamily: 'Arial, sans-serif' },
    input: { marginBottom: '20px' },
    previewContainer: { margin: '20px auto', maxWidth: '300px', border: '2px solid #ddd', borderRadius: '8px', overflow: 'hidden' },
    image: { width: '100%', display: 'block' },
    button: { padding: '10px 20px', backgroundColor: '#28a745', color: '#fff', border: 'none', borderRadius: '5px', cursor: 'pointer' },
    statusText: { marginTop: '20px', fontWeight: 'bold', color: '#555' }
};

export default FileUpload;