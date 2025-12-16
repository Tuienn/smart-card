const express = require('express');
const router = express.Router();
const Game = require('../models/Game');

// GET all games
router.get('/', async (req, res) => {
  try {
    const games = await Game.find();
    res.json({ success: true, data: games });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// GET game by ID
router.get('/:id', async (req, res) => {
  try {
    const game = await Game.findById(req.params.id);
    if (!game) {
      return res.status(404).json({ success: false, message: 'Game not found' });
    }
    res.json({ success: true, data: game });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// POST create new game
router.post('/', async (req, res) => {
  try {
    const game = new Game(req.body);
    await game.save();
    res.status(201).json({ success: true, data: game });
  } catch (error) {
    res.status(400).json({ success: false, message: error.message });
  }
});

// PUT update game
router.put('/:id', async (req, res) => {
  try {
    const game = await Game.findByIdAndUpdate(
      req.params.id,
      req.body,
      { new: true, runValidators: true }
    );
    if (!game) {
      return res.status(404).json({ success: false, message: 'Game not found' });
    }
    res.json({ success: true, data: game });
  } catch (error) {
    res.status(400).json({ success: false, message: error.message });
  }
});

// DELETE game
router.delete('/:id', async (req, res) => {
  try {
    const game = await Game.findByIdAndDelete(req.params.id);
    if (!game) {
      return res.status(404).json({ success: false, message: 'Game not found' });
    }
    res.json({ success: true, message: 'Game deleted successfully' });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

module.exports = router;
