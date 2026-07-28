const express = require('express');
const app = express();
const PORT = process.env.PORT || 3000;

// Root route
app.get('/', (req, res) => {
  res.send('Note Server is running!');
});

// Health check route
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    timestamp: new Date().toISOString()
  });
});

// Demo API route
app.get('/api/demo', (req, res) => {
  res.json({ message: 'hello world' });
});

// Start server
app.listen(PORT, () => {
  console.log(`Note Server listening on port ${PORT}`);
});
