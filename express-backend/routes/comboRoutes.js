const express = require('express');
const router = express.Router();
const Combo = require('../models/Combo');

// GET all combos
router.get('/', async (req, res) => {
  try {
    const combos = await Combo.find().populate('game_ids');
    res.json({ success: true, data: combos });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// GET combo by ID
router.get('/:id', async (req, res) => {
  try {
    const combo = await Combo.findById(req.params.id);
    if (!combo) {
      return res.status(404).json({ success: false, message: 'Combo not found' });
    }
    
    // Return combo with game_ids as array of numbers (not populated)
    // This is needed for JavaCard to receive just the IDs
    res.json({ 
      success: true, 
      data: {
        _id: combo._id,
        name: combo.name,
        description: combo.description,
        priceVND: combo.priceVND,
        discountPercentage: combo.discountPercentage,
        games: combo.game_ids  // Return as array of IDs
      }
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// POST create new combo
router.post('/', async (req, res) => {
  try {
    // Tự động tạo _id nếu không có
    if (!req.body._id) {
      // Tìm ID lớn nhất hiện tại và tăng lên 1
      const maxCombo = await Combo.findOne().sort({ _id: -1 }).limit(1);
      req.body._id = maxCombo ? maxCombo._id + 1 : 1;
    }
    
    const combo = new Combo(req.body);
    await combo.save();
    await combo.populate('game_ids');
    res.status(201).json({ success: true, data: combo });
  } catch (error) {
    res.status(400).json({ success: false, message: error.message });
  }
});

// PUT update combo
router.put('/:id', async (req, res) => {
  try {
    const combo = await Combo.findByIdAndUpdate(
      req.params.id,
      req.body,
      { new: true, runValidators: true }
    ).populate('game_ids');
    if (!combo) {
      return res.status(404).json({ success: false, message: 'Combo not found' });
    }
    res.json({ success: true, data: combo });
  } catch (error) {
    res.status(400).json({ success: false, message: error.message });
  }
});

// DELETE combo
router.delete('/:id', async (req, res) => {
  try {
    const combo = await Combo.findByIdAndDelete(req.params.id);
    if (!combo) {
      return res.status(404).json({ success: false, message: 'Combo not found' });
    }
    res.json({ success: true, message: 'Combo deleted successfully' });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

module.exports = router;
