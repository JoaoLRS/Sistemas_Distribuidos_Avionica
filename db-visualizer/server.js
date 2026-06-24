const express = require('express');
const path = require('path');

const app = express();
const port = process.env.PORT || 8081;

// Servindo arquivos estáticos do diretório public
app.use(express.static(path.join(__dirname, 'public')));

// Fallback para SPA (se necessário, embora seja uma página única)
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(port, () => {
  console.log(`📡 DB Visualizer (Frontend Estático) ativo na porta ${port}`);
});
