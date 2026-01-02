const express = require('express');
const router = express.Router();
const Transaction = require('../models/Transaction');

// GET statistics: Revenue by month
router.get('/revenue-by-month', async (req, res) => {
  try {
    const { year } = req.query;
    const currentYear = year ? parseInt(year) : new Date().getFullYear();

    const stats = await Transaction.aggregate([
      {
        $match: {
          time_stamp: {
            $gte: new Date(`${currentYear}-01-01`),
            $lt: new Date(`${currentYear + 1}-01-01`)
          }
        }
      },
      {
        $group: {
          _id: { $month: '$time_stamp' },
          totalRevenue: { $sum: '$payment' },
          transactionCount: { $sum: 1 }
        }
      },
      {
        $sort: { _id: 1 }
      },
      {
        $project: {
          month: '$_id',
          totalRevenue: 1,
          transactionCount: 1,
          _id: 0
        }
      }
    ]);

    // Fill missing months with 0
    const monthlyData = Array.from({ length: 12 }, (_, i) => {
      const monthData = stats.find(s => s.month === i + 1);
      return {
        month: i + 1,
        totalRevenue: monthData ? monthData.totalRevenue : 0,
        transactionCount: monthData ? monthData.transactionCount : 0
      };
    });

    res.json({ success: true, data: monthlyData, year: currentYear });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// GET statistics: Top 10 games
router.get('/top-games', async (req, res) => {
  try {
    const stats = await Transaction.aggregate([
      {
        $match: {
          game_id: { $ne: null }
        }
      },
      {
        $group: {
          _id: '$game_id',
          totalRevenue: { $sum: '$payment' },
          purchaseCount: { $sum: 1 }
        }
      },
      {
        $sort: { purchaseCount: -1 }
      },
      {
        $limit: 10
      },
      {
        $lookup: {
          from: 'games',
          localField: '_id',
          foreignField: '_id',
          as: 'gameInfo'
        }
      },
      {
        $unwind: {
          path: '$gameInfo',
          preserveNullAndEmptyArrays: true
        }
      },
      {
        $project: {
          gameId: '$_id',
          gameName: { $ifNull: ['$gameInfo.name', 'Unknown Game'] },
          totalRevenue: 1,
          purchaseCount: 1,
          _id: 0
        }
      }
    ]);

    res.json({ success: true, data: stats });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// GET statistics: Top 10 combos
router.get('/top-combos', async (req, res) => {
  try {
    const stats = await Transaction.aggregate([
      {
        $match: {
          combo_id: { $ne: null }
        }
      },
      {
        $group: {
          _id: '$combo_id',
          totalRevenue: { $sum: '$payment' },
          purchaseCount: { $sum: 1 }
        }
      },
      {
        $sort: { purchaseCount: -1 }
      },
      {
        $limit: 10
      },
      {
        $lookup: {
          from: 'combos',
          localField: '_id',
          foreignField: '_id',
          as: 'comboInfo'
        }
      },
      {
        $unwind: {
          path: '$comboInfo',
          preserveNullAndEmptyArrays: true
        }
      },
      {
        $project: {
          comboId: '$_id',
          comboName: { $ifNull: ['$comboInfo.name', 'Unknown Combo'] },
          totalRevenue: 1,
          purchaseCount: 1,
          _id: 0
        }
      }
    ]);

    res.json({ success: true, data: stats });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// GET statistics: Revenue by age group
router.get('/revenue-by-age', async (req, res) => {
  try {
    const stats = await Transaction.aggregate([
      {
        $match: {
          user_age: { $gte: 1, $lte: 80 }
        }
      },
      {
        $group: {
          _id: '$user_age',
          totalRevenue: { $sum: '$payment' },
          transactionCount: { $sum: 1 },
          avgRevenue: { $avg: '$payment' }
        }
      },
      {
        $project: {
          ageGroup: { $toString: '$_id' },
          totalRevenue: 1,
          transactionCount: 1,
          avgRevenue: { $round: ['$avgRevenue', 0] },
          _id: 0
        }
      },
      {
        $sort: { ageGroup: 1 }
      }
    ]);

    res.json({ success: true, data: stats });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// GET statistics: Summary (overview)
router.get('/summary', async (req, res) => {
  try {
    const summary = await Transaction.aggregate([
      {
        $facet: {
          totalStats: [
            {
              $group: {
                _id: null,
                totalRevenue: { $sum: '$payment' },
                totalTransactions: { $sum: 1 },
                avgTransaction: { $avg: '$payment' }
              }
            }
          ],
          typeStats: [
            {
              $group: {
                _id: {
                  $cond: [
                    { $ne: ['$combo_id', null] }, 'Combo',
                    { $cond: [{ $ne: ['$game_id', null] }, 'Game', 'Topup'] }
                  ]
                },
                count: { $sum: 1 },
                revenue: { $sum: '$payment' }
              }
            }
          ]
        }
      }
    ]);

    const result = {
      total: summary[0].totalStats[0] || { totalRevenue: 0, totalTransactions: 0, avgTransaction: 0 },
      byType: summary[0].typeStats
    };

    res.json({ success: true, data: result });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

module.exports = router;
