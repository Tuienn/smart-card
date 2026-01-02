require("dotenv").config();
const express = require("express");
const cors = require("cors");
const morgan = require("morgan");
const connectDB = require("./config/database");
const seedDatabase = require("./config/seedData");

// Import routes
const gameRoutes = require("./routes/gameRoutes");
const comboRoutes = require("./routes/comboRoutes");
const cardRoutes = require("./routes/cardRoutes");
const transactionRoutes = require("./routes/transactionRoutes");
const momoRoutes = require("./routes/momoRoutes");
const statisticsRoutes = require("./routes/statisticsRoutes");
const adminAuthRoutes = require("./routes/adminAuthRoutes");

const app = express();
const PORT = process.env.PORT || 4000;

// Connect to MongoDB và seed dữ liệu
connectDB().then(async () => {
  await seedDatabase();
});

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(morgan("dev"));

// Routes
app.get("/", (req, res) => {
  res.json({
    message: "SmartCard Backend API",
    version: "1.0.0",
    endpoints: {
      games: "/api/games",
      combos: "/api/combos",
      cards: "/api/cards",
      transactions: "/api/transactions",
      momo: "/api/momo",
      statistics: "/api/statistics",
      admin: "/api/admin",
    },
  });
});

// API Routes
app.use("/api/games", gameRoutes);
app.use("/api/combos", comboRoutes);
app.use("/api/cards", cardRoutes);
app.use("/api/transactions", transactionRoutes);
app.use("/api/momo", momoRoutes);
app.use("/api/statistics", statisticsRoutes);
app.use("/api/admin", adminAuthRoutes);

// Error handling middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({
    success: false,
    message: "Something went wrong!",
    error: process.env.NODE_ENV === "development" ? err.message : undefined,
  });
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({
    success: false,
    message: "Route not found",
  });
});

// Start server
app.listen(PORT, () => {
  console.log(`🚀 Server is running on http://localhost:${PORT}`);
  console.log(`📝 Environment: ${process.env.NODE_ENV || "development"}`);
});
