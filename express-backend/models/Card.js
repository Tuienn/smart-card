const mongoose = require('mongoose');

const cardSchema = new mongoose.Schema({
  _id: {
    type: String,
    required: true
  },
  user_name: {
    type: String,
    required: true,
    trim: true
  },
  user_age: {
    type: Number,
    required: true,
    min: 0
  },
  user_gender: {
    type: Boolean,
    required: true
  },
  public_key: {
    type: String,
    required: true,
    unique: true
  }
}, {
  timestamps: true,
  _id: false  // Disable auto-generated _id
});

module.exports = mongoose.model('Card', cardSchema);
