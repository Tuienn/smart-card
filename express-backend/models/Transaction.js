const mongoose = require('mongoose');

const transactionSchema = new mongoose.Schema({
  card_id: {
    type: String,  // Phải là String vì Card._id là String
    required: true
  },
  user_age: {
    type: Number,
    required: true,
    min: 0,
    comment: 'Tuổi user tại thời điểm giao dịch (snapshot)'
  },
  payment: {
    type: Number,
    required: true,
    min: 0
  },
  time_stamp: {
    type: Date,
    default: Date.now
  },
  game_id: {
    type: Number,
    ref: 'Game',
    default: null
  },
  combo_id: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Combo',
    default: null
  }
}, {
  timestamps: true
});

module.exports = mongoose.model('Transaction', transactionSchema);
