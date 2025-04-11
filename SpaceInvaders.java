import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.sound.sampled.*;
import java.io.*;

public class SpaceInvaders extends JPanel implements ActionListener, KeyListener {

    void playSound(String fileName) {
        try {
            File soundFile = new File(fileName);
            if (!soundFile.exists()) {
            System.out.println("Sound file not found: " + fileName);
            return;
        }
            AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioInput);
            clip.start();
        
    } catch (Exception e) {
            e.printStackTrace();
        }
    }
    Timer timer;
    Player player;
    Player clonePlayer = null;
    ArrayList<Bullet> bullets = new ArrayList<>();
    ArrayList<Enemy> enemies = new ArrayList<>();
    Boss boss = null;
    boolean gameOver = false;
    boolean canShoot = true;
    boolean pausedForUpgrade = false;
    int score = 0;
    int stage = 1;
    double enemySpeed = 4.0; // Increased enemy speed again (2x more)

    boolean upgradeDoubleShot = false;
    boolean upgradePiercing = false;
    boolean upgradeSpeed = false;
    boolean upgradeClone = false;

    public SpaceInvaders() {
        setPreferredSize(new Dimension(1600, 1200));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        player = new Player(375, 1000);
        spawnEnemies();
        timer = new Timer(15, this);
        timer.start();
    }

    void spawnEnemies() {
        enemies.clear();
        boss = null;
        if (stage == 6) {
            boss = new Boss(700, 100);
            return;
        }
        Color enemyColor = switch (stage) {
            case 2 -> Color.GREEN;
            case 3 -> Color.BLUE;
            case 4 -> Color.ORANGE;
            case 5 -> Color.WHITE;
            default -> Color.RED;
        };
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 10; j++) {
                enemies.add(new Enemy(60 + j * 60, 50 + i * 40, enemySpeed, enemyColor));
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.drawString("Score: " + score, 20, 30);
        g.drawString("Stage: " + stage, 700, 30);
        if (stage == 5 && enemies.isEmpty() && boss == null) {
            g.drawString("5. Final Stage Unlocked", 700, 80);
        }

        if (gameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 50));
            g.setColor(Color.RED);
            g.drawString("GAME OVER,", 550, 500);
            g.drawString("  YOU WIN!", 550, 570);
            return;
        
        }

        if (pausedForUpgrade) {
            if (stage == 5) {
                g.setColor(Color.RED);
                g.drawString("Press 5 to skip upgrades and face the boss!", 400, 750);
            }
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.drawString("Upgrade Available! Press 1-4 to Choose:", 400, 500);
            g.setColor(upgradeClone ? Color.GRAY : Color.YELLOW);
            g.drawString("1. Add second ship", 400, 550);
            g.setColor(upgradeDoubleShot ? Color.GRAY : Color.YELLOW);
            g.drawString("2. Double shot", 400, 600);
            g.setColor(upgradePiercing ? Color.GRAY : Color.YELLOW);
            g.drawString("3. Piercing bullets", 400, 650);
            g.setColor(upgradeSpeed ? Color.GRAY : Color.YELLOW);
            g.drawString("4. Double speed", 400, 700);
            return;
        }

        player.draw(g);
        if (clonePlayer != null) clonePlayer.draw(g);
        for (Bullet b : bullets) b.draw(g);
        for (Enemy e : enemies) e.draw(g);
        if (boss != null) boss.draw(g);
    }

    public void actionPerformed(ActionEvent e) {
        if (gameOver || pausedForUpgrade) return;
        player.move();
        if (clonePlayer != null) {
            clonePlayer.dx = player.dx;
            clonePlayer.move();
        }
        bullets.forEach(Bullet::move);
        bullets.removeIf(b -> b.y < 0);
        for (Enemy enemy : enemies) {
            enemy.move();
            if (enemy.y > 1000) gameOver = true;
        }
        if (boss != null) {
            boss.move();
            for (int i = 0; i < bullets.size(); i++) {
                Bullet b = bullets.get(i);
                if (b.getBounds().intersects(boss.getBounds())) {
                    if (!b.pierced) {
                        bullets.remove(i);
                        i--;
                    } else {
                        b.pierceCount++;
                        if (b.pierceCount >= 2) {
                            bullets.remove(i);
                            i--;
                        }
                    }
                    boss.hp--;
                    score += 200;
                    if (boss.hp <= 0) {
                        playSound("win.wav");
                        boss = null;
                        gameOver = true;
                    }
                    break;
                }
            }
        }
        for (int i = 0; i < bullets.size(); i++) {
            Bullet b = bullets.get(i);
            for (int j = 0; j < enemies.size(); j++) {
                Enemy enemy = enemies.get(j);
                if (b.getBounds().intersects(enemy.getBounds())) {
                    if (!b.pierced) {
                        bullets.remove(i);
                        i--;
                    } else {
                        b.pierceCount++;
                        if (b.pierceCount >= 2) {
                            bullets.remove(i);
                            i--;
                        }
                    }
                    enemies.remove(j);
                    playSound("explode.wav");
                    score += (int)(100 * Math.pow(2, stage - 1));
                    break;
                }
            }
        }
        if (enemies.isEmpty() && boss == null && stage < 6) pausedForUpgrade = true;
        repaint();
    }

    public void keyPressed(KeyEvent e) {
        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_SHIFT) {
            enemies.clear();
            return;
        }
        int key = e.getKeyCode();
        if (pausedForUpgrade) {
            if (key == KeyEvent.VK_1 && !upgradeClone) {
                clonePlayer = new Player(player.x + 60, 1000); // place next to original player
                upgradeClone = true;
                advanceStage();
            } else if (key == KeyEvent.VK_2 && !upgradeDoubleShot) {
                upgradeDoubleShot = true;
                advanceStage();
            } else if (key == KeyEvent.VK_3 && !upgradePiercing) {
                upgradePiercing = true;
                advanceStage();
            } else if (key == KeyEvent.VK_4 && !upgradeSpeed) {
                upgradeSpeed = true;
                advanceStage();
            } else if (key == KeyEvent.VK_5 && stage == 5) {
                stage = 6;
                pausedForUpgrade = false;
                spawnEnemies();
            }
            return;
        }
        if (key == KeyEvent.VK_LEFT) player.dx = upgradeSpeed ? -10 : -5;
        if (key == KeyEvent.VK_RIGHT) player.dx = upgradeSpeed ? 10 : 5;
        if (key == KeyEvent.VK_SPACE && canShoot) {
            canShoot = false;
            fireBullet(player);
            if (clonePlayer != null) fireBullet(clonePlayer);
        }
    }

    void fireBullet(Player p) {
        playSound("shoot.wav");
        bullets.add(new Bullet(p.x + 15, p.y, upgradePiercing));
        if (upgradeDoubleShot) bullets.add(new Bullet(p.x + 5, p.y, upgradePiercing));
    }

    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) player.dx = 0;
        if (key == KeyEvent.VK_SPACE) canShoot = true;
    }

    public void keyTyped(KeyEvent e) {}

    void advanceStage() {
        stage++;
        pausedForUpgrade = false;
        spawnEnemies();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Space Invaders");
        SpaceInvaders game = new SpaceInvaders();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    class Player {
        int x, y, dx;
        Player(int x, int y) {
            this.x = x;
            this.y = y;
        }
        void move() {
            x += dx;
            x = Math.max(0, Math.min(1570, x));
        }
        void draw(Graphics g) {
            g.setColor(Color.GREEN);
            g.fillRect(x, y, 30, 20);
        }
    }

    class Bullet {
        int x, y;
        boolean pierced;
        int pierceCount = 0;
        Bullet(int x, int y, boolean pierced) {
            this.x = x;
            this.y = y;
            this.pierced = pierced;
        }
        void move() { y -= 10; }
        void draw(Graphics g) {
            g.setColor(pierced ? Color.RED : Color.YELLOW);
            g.fillRect(x, y, 5, 10);
        }
        Rectangle getBounds() {
            return new Rectangle(x, y, 5, 10);
        }
    }

    class Enemy {
        int x, y;
        double dx;
        Color color;
        Enemy(int x, int y, double speed, Color color) {
            this.x = x;
            this.y = y;
            this.dx = speed;
            this.color = color;
        }
        void move() {
            x += dx;
            if (x <= 0 || x >= 1570) {
                dx = -dx;
                y += 20;
            }
        }
        void draw(Graphics g) {
            g.setColor(color);
            g.fillRect(x, y, 30, 20);
        }
        Rectangle getBounds() {
            return new Rectangle(x, y, 30, 20);
        }
    }

    class Boss {
        int x, y, hp = 150;
        int dx = 3;
        Boss(int x, int y) {
            this.x = x;
            this.y = y;
        }
        void move() {
            x += dx;
            if (x <= 0 || x >= 1600 - 8 * 36) {
                dx = -dx;
            }
        }
        void draw(Graphics g) {
            g.setColor(Color.RED);
            int[][] skull = {
                {1, 1, 1, 1, 1, 1, 1, 1},
                {1, 0, 1, 1, 1, 1, 0, 1},
                {1, 0, 1, 1, 1, 1, 0, 1},
                {1, 1, 1, 1, 1, 1, 1, 1},
                {1, 1, 0, 1, 1, 0, 1, 1},
                {1, 1, 1, 0, 0, 1, 1, 1}
            };
            int blockSize = 36;
            for (int i = 0; i < skull.length; i++) {
                for (int j = 0; j < skull[i].length; j++) {
                    if (skull[i][j] == 1) {
                        g.fillRect(x + j * blockSize, y + i * blockSize, blockSize, blockSize);
                    }
                }
            }
            g.setColor(Color.WHITE);
            g.drawString("BOSS HP: " + hp, x + 10, y - 10);
        }
        Rectangle getBounds() {
            return new Rectangle(x, y, 8 * 36, 6 * 36);
        }
    }
}
