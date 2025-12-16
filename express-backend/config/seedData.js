const Game = require('../models/Game');
const Combo = require('../models/Combo');

// Dữ liệu mẫu cho các trò chơi
const gamesData = [
  {
    _id: 1,
    name: 'Tàu lượn siêu tốc',
    price: 50000,
    description: 'Trải nghiệm tốc độ và cảm giác mạnh với tàu lượn hiện đại, nhiều vòng xoáy 360 độ'
  },
  {
    _id: 2,
    name: 'Cưỡi bò điện',
    price: 30000,
    description: 'Thử thách khả năng giữ thăng bằng trên lưng bò điện với nhiều cấp độ khó khác nhau'
  },
  {
    _id: 3,
    name: 'Vòng xoay thần tốc',
    price: 40000,
    description: 'Cảm giác mạnh với vòng xoay 360 độ ở tốc độ cao, phù hợp với người mạo hiểm'
  },
  {
    _id: 4,
    name: 'Game thực tế ảo VR',
    price: 60000,
    description: 'Trải nghiệm thực tế ảo với công nghệ VR hiện đại, nhiều game hành động và phiêu lưu'
  },
  {
    _id: 5,
    name: 'Nhà ma ám',
    price: 35000,
    description: 'Khám phá ngôi nhà ma đầy rẫy bất ngờ và cảm giác hồi hộp'
  },
  {
    _id: 6,
    name: 'Xe điện va chạm',
    price: 25000,
    description: 'Lái xe điện và va chạm vui vẻ với bạn bè trong không gian an toàn'
  },
  {
    _id: 7,
    name: 'Tháp rơi tự do',
    price: 55000,
    description: 'Trải nghiệm cảm giác rơi tự do từ độ cao 50m trong vài giây nghẹt thở'
  },
  {
    _id: 8,
    name: 'Đu quay khổng lồ',
    price: 35000,
    description: 'Ngắm nhìn toàn cảnh khu vui chơi từ độ cao 40m trên đu quay khổng lồ'
  },
  {
    _id: 9,
    name: 'Game bắn súng 3D',
    price: 45000,
    description: 'Thi đấu bắn súng 3D với màn hình lớn và hiệu ứng âm thanh sống động'
  },
  {
    _id: 10,
    name: 'Đua xe mô phỏng',
    price: 50000,
    description: 'Trải nghiệm đua xe F1 với cabin mô phỏng chân thực và nhiều đường đua nổi tiếng'
  }
];

// Dữ liệu mẫu cho các combo
const combosData = [
  {
    _id: 1,
    name: 'Combo Cảm Giác Mạnh',
    price: 150000,
    description: 'Trọn gói 3 trò chơi cảm giác mạnh nhất: Tàu lượn, Tháp rơi tự do, Vòng xoay thần tốc',
    game_ids: [1, 7, 3] // Tàu lượn siêu tốc, Tháp rơi tự do, Vòng xoay thần tốc
  },
  {
    _id: 2,
    name: 'Combo Công Nghệ',
    price: 120000,
    description: 'Trải nghiệm công nghệ hiện đại với VR và game bắn súng 3D',
    game_ids: [4, 9, 10] // Game thực tế ảo VR, Game bắn súng 3D, Đua xe mô phỏng
  },
  {
    _id: 3,
    name: 'Combo Gia Đình',
    price: 90000,
    description: 'Vui chơi an toàn cho cả gia đình với 3 trò chơi nhẹ nhàng',
    game_ids: [6, 8, 2] // Xe điện va chạm, Đu quay khổng lồ, Cưỡi bò điện
  },
  {
    _id: 4,
    name: 'Combo Toàn Diện',
    price: 220000,
    description: 'Trải nghiệm tất cả 5 trò chơi hot nhất trong ngày',
    game_ids: [1, 4, 7, 10, 5] // Tàu lượn siêu tốc, Game thực tế ảo VR, Tháp rơi tự do, Đua xe mô phỏng, Nhà ma ám
  },
  {
    _id: 5,
    name: 'Combo Sinh Viên',
    price: 100000,
    description: 'Ưu đãi đặc biệt cho sinh viên với 3 trò chơi thú vị',
    game_ids: [2, 9, 5] // Cưỡi bò điện, Game bắn súng 3D, Nhà ma ám
  }
];

/**
 * Seed dữ liệu games và combos vào database
 */
async function seedDatabase() {
  try {
    // Kiểm tra xem đã có dữ liệu chưa
    const gamesCount = await Game.countDocuments();
    const combosCount = await Combo.countDocuments();

    if (gamesCount > 0 || combosCount > 0) {
      console.log('📊 Database đã có dữ liệu, bỏ qua seeding');
      console.log(`   - Games: ${gamesCount} trò chơi`);
      console.log(`   - Combos: ${combosCount} combo`);
      return;
    }

    console.log('🌱 Bắt đầu seed dữ liệu...');

    // Insert games trước
    const games = await Game.insertMany(gamesData);
    console.log(`✅ Đã thêm ${games.length} trò chơi`);

    // Insert combos (đã có game_ids cố định)
    const combos = await Combo.insertMany(combosData);
    console.log(`✅ Đã thêm ${combos.length} combo`);

    console.log('🎉 Seed dữ liệu hoàn tất!');
    
  } catch (error) {
    console.error('❌ Lỗi khi seed dữ liệu:', error.message);
    throw error;
  }
}

module.exports = seedDatabase;
