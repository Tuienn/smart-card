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
  price: {
    type: Number,
    required: true,
    min: 0
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
