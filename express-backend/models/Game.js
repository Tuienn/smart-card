const mongoose = require('mongoose');

const gameSchema = new mongoose.Schema({
  _id: {
    type: Number,
    required: true
  },
  name: {
    type: String,
    required: true,
    trim: true
  },
  points: {
    type: Number,
    required: true,
    min: 0,
    comment: 'Điểm cần để chơi (100k = 10 điểm)'
  },
  description: {
    type: String,
    required: true
  }
}, {
  timestamps: true,
  _id: false
});

module.exports = mongoose.model('Game', gameSchema);
