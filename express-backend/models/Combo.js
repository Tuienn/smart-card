const mongoose = require('mongoose');

const comboSchema = new mongoose.Schema({
  _id: {
    type: Number,
    required: true
  },
  name: {
    type: String,
    required: true,
    trim: true
  },
  priceVND: {
    type: Number,
    required: true,
    min: 0,
    comment: 'Giá tiền thật (VNĐ)'
  },
  discountPercentage: {
    type: Number,
    required: true,
    min: 0,
    max: 100,
    comment: 'Phần trăm giảm giá so với mua lẻ bằng điểm'
  },
  description: {
    type: String,
    default: ''
  },
  game_ids: [{
    type: Number,
    ref: 'Game'
  }]
}, {
  timestamps: true,
  _id: false
});

module.exports = mongoose.model('Combo', comboSchema);
