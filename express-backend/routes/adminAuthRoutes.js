const express = require('express');
const router = express.Router();
const Admin = require('../models/Admin');

// POST /api/admin/login - Admin login
router.post('/login', async (req, res) => {
    try {
        const { username, password } = req.body;

        if (!username || !password) {
            return res.status(400).json({ 
                success: false, 
                message: 'Username và password không được để trống' 
            });
        }

        // Find admin by username
        const admin = await Admin.findOne({ username });

        if (!admin) {
            return res.status(401).json({ 
                success: false, 
                message: 'Tên đăng nhập hoặc mật khẩu không đúng' 
            });
        }

        // Check password (simple comparison - in production use bcrypt)
        if (admin.password !== password) {
            return res.status(401).json({ 
                success: false, 
                message: 'Tên đăng nhập hoặc mật khẩu không đúng' 
            });
        }

        // Update last login
        admin.lastLogin = new Date();
        await admin.save();

        res.json({ 
            success: true, 
            message: 'Đăng nhập thành công',
            data: {
                username: admin.username,
                lastLogin: admin.lastLogin
            }
        });

    } catch (error) {
        res.status(500).json({ 
            success: false, 
            message: error.message 
        });
    }
});

// POST /api/admin/register - Create new admin (optional)
router.post('/register', async (req, res) => {
    try {
        const { username, password } = req.body;

        if (!username || !password) {
            return res.status(400).json({ 
                success: false, 
                message: 'Username và password không được để trống' 
            });
        }

        // Check if admin already exists
        const existingAdmin = await Admin.findOne({ username });
        if (existingAdmin) {
            return res.status(400).json({ 
                success: false, 
                message: 'Username đã tồn tại' 
            });
        }

        // Create new admin
        const admin = new Admin({
            username,
            password // In production, hash this with bcrypt
        });

        await admin.save();

        res.json({ 
            success: true, 
            message: 'Tạo tài khoản admin thành công',
            data: {
                username: admin.username
            }
        });

    } catch (error) {
        res.status(500).json({ 
            success: false, 
            message: error.message 
        });
    }
});

module.exports = router;
