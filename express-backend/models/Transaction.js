const mongoose = require('mongoose');

const transactionSchema = new mongoose.Schema({
  card_id: {
    type: String,  // Phải là String vì Card._id là String
    ref: 'Card',
    required: true
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
  combo_id: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Combo',
    default: null
  }
}, {
  timestamps: true
});

module.exports = mongoose.model('Transaction', transactionSchema);
