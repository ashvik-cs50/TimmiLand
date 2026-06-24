/*
 * Decompiled with CFR 0.152.
 */
package com.sigmarizz;

import com.sigmarizz.NetState;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBEasyFont;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.glfw.GLFWVidMode;

public class Main {
    private long window;
    private int winW = 1280;
    private int winH = 720;
    private static final String DISCORD_ID = "1484047342293487616";
    private static final int TYPE_SWORD = 0;
    private static final int TYPE_FOOD = 2;
    private static final int TYPE_LOG = 3;
    private static final int TYPE_BOW = 4;
    private static final int TYPE_ARROW = 5;
    private final String[] skins = new String[]{"Default", "Ghost", "Radioactive", "Red", "Blue"};
    private int skinIndex = 0;
    private boolean skinSwitchHeld = false;
    private float pX = 0.0f;
    private float pY = 0.0f;
    private float pZ = 0.0f;
    private float velY = 0.0f;
    private float hp = 20.0f;
    private float hunger = 20.0f;
    private float stamina = 100.0f;
    private float xp = 0.0f;
    private float targetYaw = 0.0f;
    private float currentYaw = 0.0f;
    private float targetPitch = 25.0f;
    private float currentPitch = 25.0f;
    private float lastMouseX;
    private float lastMouseY;
    private float zoomLevel = 1.0f;
    private float damageFlash = 0f;
    private boolean isRightClickDown = false;
    private boolean isAttacking = false;
    private boolean isMoving = false;
    private boolean isFirstPerson = false;
    private final float GRAVITY = -0.012f;
    private final float JUMP_FORCE = 0.25f;
    private ItemStack[] inventory = new ItemStack[5];
    private int selectedSlot = 0;
    private List<Animal> animals = new ArrayList<Animal>();
    private List<Tree> trees = new ArrayList<Tree>();
    private List<Projectile> arrows = new ArrayList<Projectile>();
    private List<Zombie> zombies = new ArrayList<>();
    private int splashTex;
    private boolean showSplash = true;
    private final String playerName = System.getenv().getOrDefault("PLAYER_NAME", "Player1");
    private long lastNetSync = 0L;
    private List<NetState.Player> others = new ArrayList<NetState.Player>();
    private int[] walkFrames = new int[5];
    private int[] attackFrames = new int[4];
    private int[] defaultAttackFrames = new int[4];
    private int item1;
    private int item2;
    private int item3;
    private int cowTex;
    private int pigTex;
    private int grassTex;
    private int logTex;
    private int woodTex;
    private int meatTex;
    private int arrowTex;
    private int bowTex;
    private int heartFullTex;
    private int heartHalfTex;
    private int hungFullTex;
    private int hungHalfTex;
    private int curFrame = 0;
    private long lastAnimTime = 0L;
    private long lastTickTime = 0L;

    // ===== MOBILE UI PLATFORM FLAGS & COORDINATES =====
    private boolean isMobile = false;
    private float joystickCenterX = 150.0f;
    private float joystickCenterY = 550.0f;
    private float joystickRadius = 80.0f;
    private float knobX = 0.0f; // Current horizontal vector (-1.0 to 1.0)
    private float knobY = 0.0f; // Current vertical vector (-1.0 to 1.0)
    private boolean isJoystickActive = false;
    private float jumpBtnX = 1100.0f;
    private float jumpBtnY = 550.0f;
    private float jumpBtnW = 120.0f;
    private float jumpBtnH = 60.0f;
    private boolean isJumpPressed = false;

    public void run() {
        this.init();
        this.loop();
        this.cleanup();
    }

    private void init() {
        int i;

        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("GLFW failed");
        }

        // Detect if executing on an embedded ARM system configuration context (Android/iOS environment properties)
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("android") || os.contains("ios") || System.getenv("MOBILE_BUILD") != null) {
            this.isMobile = true;
        }

        // Force evaluation bypass simulation for direct local PC testing
        // this.isMobile = true;

        // ===== FULLSCREEN SETUP =====
        boolean fullscreen = true;
        long monitor = fullscreen ? GLFW.glfwGetPrimaryMonitor() : 0L;

        if (fullscreen) {
            GLFWVidMode vid = GLFW.glfwGetVideoMode(monitor);
            this.winW = vid.width();
            this.winH = vid.height();
        }

        this.window = GLFW.glfwCreateWindow(this.winW, this.winH, "TimmiLand Alpha", monitor, 0L);

        GLFW.glfwMakeContextCurrent(this.window);
        GLFW.glfwSwapInterval(1);
        GL.createCapabilities();

        if (!fullscreen) {
            GLFW.glfwSetWindowPos(this.window, 100, 100);
        }

        // Dynamically adjust responsive coordinate bounds for variable touch anchors based on active displays
        this.joystickCenterX = 150.0f;
        this.joystickCenterY = this.winH - 150.0f;
        this.jumpBtnX = this.winW - 180.0f;
        this.jumpBtnY = this.winH - 140.0f;

        GL11.glClearColor(0.45f, 0.65f, 0.95f, 1.0f);

        GLFW.glfwSetScrollCallback(this.window, (win, xoff, yoff) -> {
            this.zoomLevel = (float)((double)this.zoomLevel - yoff * 0.1);
            this.zoomLevel = Math.max(0.0f, Math.min(2.5f, this.zoomLevel));
        });

        // ===== TEXTURES =====
        for (int i2 = 0; i2 < 4; ++i2) {
            this.defaultAttackFrames[i2] = this.loadTexture(i2 + 1 + ".gif");
        }

        System.arraycopy(this.defaultAttackFrames, 0, this.attackFrames, 0, 4);
        this.loadSkin(this.skinIndex);

        this.item1 = this.loadTexture("item1.png");
        this.item2 = this.loadTexture("item2.png");
        this.item3 = this.loadTexture("item3.png");
        this.meatTex = this.loadTexture("meat.png");
        this.logTex = this.loadTexture("log.png");
        this.bowTex = this.loadTexture("bow.png");
        this.arrowTex = this.loadTexture("arrow.png");
        this.heartFullTex = this.loadTexture("heart (2).png");
        this.heartHalfTex = this.loadTexture("heart (1).png");
        this.hungFullTex = this.loadTexture("hung-full.png");
        this.hungHalfTex = this.loadTexture("hung-half.png");
        this.grassTex = this.loadTexture("grass.png");
        this.woodTex = this.loadTexture("wood.png");
        this.cowTex = this.loadTexture("cow.png");
        this.pigTex = this.loadTexture("pig.png");
        this.splashTex = this.loadTexture("splash (2).png");

        // ===== INVENTORY =====
        this.inventory[0] = new ItemStack("Sword", this.item3, 0, 1);
        this.inventory[1] = new ItemStack("Meat", this.meatTex, 2, 5);
        this.inventory[2] = new ItemStack("Bow", this.bowTex, 4, 1);
        this.inventory[3] = new ItemStack("Arrow", this.arrowTex, 5, 24);
        this.inventory[4] = new ItemStack("Log", this.logTex, 3, 3);

        Random r = new Random();

        // ===== ANIMALS =====
        for (i = 0; i < 30; ++i) {
            this.animals.add(new Animal(
                    r.nextFloat() * 400.0f - 200.0f,
                    r.nextFloat() * 400.0f - 200.0f,
                    r.nextInt(2)
            ));
        }

        // ===== ZOMBIES (FIXED SPAWN) =====
        for (i = 0; i < 10; i++) {
            this.zombies.add(new Zombie(
                    r.nextFloat() * 200.0f - 100.0f,
                    r.nextFloat() * 200.0f - 100.0f
            ));
        }

        // ===== TREES =====
        for (i = 0; i < 100; ++i) {
            this.trees.add(new Tree(
                    r.nextFloat() * 500.0f - 250.0f,
                    r.nextFloat() * 500.0f - 250.0f
            ));
        }

        GL11.glEnable(2929);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);

        this.initDiscord();
    }

    private void handleInput() {
        boolean bl = this.isFirstPerson = this.zoomLevel < 0.25f;

        // ===== NATIVE MULTI-TOUCH SIMULATION POLLING OVERRIDES FOR MOBILE CANVAS =====
        this.isJumpPressed = false;
        if (this.isMobile) {
            try (MemoryStack s = MemoryStack.stackPush()) {
                DoubleBuffer mx = s.mallocDouble(1);
                DoubleBuffer my = s.mallocDouble(1);
                GLFW.glfwGetCursorPos(this.window, mx, my);
                float touchX = (float) mx.get(0);
                float touchY = (float) my.get(0);

                if (GLFW.glfwGetMouseButton(this.window, 0) == 1) {
                    // Check Jump bounding box constraints mapping intersection
                    if (touchX >= this.jumpBtnX && touchX <= this.jumpBtnX + this.jumpBtnW &&
                            touchY >= this.jumpBtnY && touchY <= this.jumpBtnY + this.jumpBtnH) {
                        this.isJumpPressed = true;
                    }

                    // Check Joystick circle boundaries intersection
                    float dx = touchX - this.joystickCenterX;
                    float dy = touchY - this.joystickCenterY;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);

                    if (dist < this.joystickRadius * 2.0f || this.isJoystickActive) {
                        this.isJoystickActive = true;
                        if (dist == 0) {
                            this.knobX = 0;
                            this.knobY = 0;
                        } else {
                            float clampDist = Math.min(dist, this.joystickRadius);
                            this.knobX = (dx / dist) * (clampDist / this.joystickRadius);
                            this.knobY = (dy / dist) * (clampDist / this.joystickRadius);
                        }
                    }
                } else {
                    this.isJoystickActive = false;
                    this.knobX = 0.0f;
                    this.knobY = 0.0f;
                }
            }
        }

        // ===== PHYSICS BOUNDS / VERTICAL VELOCITY RESOLUTIONS =====
        boolean wantsToJump = (GLFW.glfwGetKey(this.window, 32) == 1) || this.isJumpPressed;
        if (wantsToJump && this.pY <= 0.01f) {
            this.velY = 0.25f;
        }

        this.pY += this.velY;
        if (this.pY > 0.0f) {
            this.velY += -0.012f;
        } else {
            this.pY = 0.0f;
            this.velY = 0.0f;
        }

        for (int i = 0; i < 5; ++i) {
            if (GLFW.glfwGetKey(this.window, 49 + i) != 1) continue;
            this.selectedSlot = i;
        }

        ItemStack cur = this.inventory[this.selectedSlot];

        // ===== LOOK CALCULATIONS (MOUSE LOOK CAPTURE ROUTINE RUNNING PRIMARILY ON PC DESKTOP STICK LOGIC EXCLUSIONS) =====
        boolean isLooking = GLFW.glfwGetMouseButton(this.window, 1) == 1;
        // On mobile configurations, look updates bypass to support drag actions when the active joystick track isn't targeted
        if (this.isMobile && GLFW.glfwGetMouseButton(this.window, 0) == 1 && !this.isJoystickActive && !this.isJumpPressed) {
            isLooking = true;
        }

        if (isLooking) {
            GLFW.glfwSetInputMode(this.window, 208897, 212995);
            try (MemoryStack s = MemoryStack.stackPush();){
                DoubleBuffer mx = s.mallocDouble(1);
                DoubleBuffer my = s.mallocDouble(1);
                GLFW.glfwGetCursorPos(this.window, mx, my);
                if (this.isRightClickDown) {
                    this.targetYaw += (float)(mx.get(0) - (double)this.lastMouseX) * 0.2f;
                    this.targetPitch = Math.max(-85.0f, Math.min(85.0f, this.targetPitch + (float)(my.get(0) - (double)this.lastMouseY) * 0.2f));
                }
                this.lastMouseX = (float)mx.get(0);
                this.lastMouseY = (float)my.get(0);
                this.isRightClickDown = true;
            }
        } else {
            GLFW.glfwSetInputMode(this.window, 208897, 212993);
            this.isRightClickDown = false;
        }

        float speed = GLFW.glfwGetKey(this.window, 340) == 1 && this.stamina > 5.0f ? 0.45f : 0.22f;
        this.isMoving = false;
        float rad = (float)Math.toRadians(this.currentYaw);
        float sn = (float)Math.sin(rad);
        float cs = (float)Math.cos(rad);

        // ===== MOVEMENT SYSTEM SPLIT =====
        if (this.isMobile) {
            // Apply analog translations directly mapped relative to the camera vector direction matrices on touch devices
            if (Math.abs(this.knobX) > 0.15f || Math.abs(this.knobY) > 0.15f) {
                // Invert the touch matrix processing vector calculation parameters to keep pace matching WASD outputs
                float forwardX = sn * (-this.knobY) + cs * this.knobX;
                float forwardZ = (-cs) * (-this.knobY) + sn * this.knobX;

                this.pX += forwardX * speed;
                this.pZ -= forwardZ * speed;
                this.isMoving = true;
            }
        } else {
            // Keyboard Core Traversal Vectors Layout Configurations Fallback
            if (GLFW.glfwGetKey(this.window, 87) == 1) {
                this.pX += sn * speed;
                this.pZ -= cs * speed;
                this.isMoving = true;
            }
            if (GLFW.glfwGetKey(this.window, 83) == 1) {
                this.pX -= sn * speed;
                this.pZ += cs * speed;
                this.isMoving = true;
            }
            if (GLFW.glfwGetKey(this.window, 65) == 1) {
                this.pX -= cs * speed;
                this.pZ -= sn * speed;
                this.isMoving = true;
            }
            if (GLFW.glfwGetKey(this.window, 68) == 1) {
                this.pX += cs * speed;
                this.pZ += sn * speed;
                this.isMoving = true;
            }
        }

        if (this.isMoving && speed > 0.3f) {
            this.stamina = Math.max(0.0f, this.stamina - 0.5f);
        }

        if (GLFW.glfwGetKey(this.window, 69) == 1 && !this.isAttacking && cur != null) {
            if (cur.type == 0) {
                this.isAttacking = true;
                this.curFrame = 0;
                this.checkCombatCollision();
            } else if (cur.type == 4) {
                if (this.consumeArrow()) {
                    this.isAttacking = true;
                    this.curFrame = 0;
                    this.arrows.add(new Projectile(this.pX, this.pY + 1.5f, this.pZ, this.currentYaw, this.currentPitch));
                }
            } else if (cur.type == 2 && this.hunger <= 19.5f) {
                this.hunger = Math.min(20.0f, this.hunger + 4.0f);
                --cur.count;
                if (cur.count <= 0) {
                    this.inventory[this.selectedSlot] = null;
                }
                this.isAttacking = true;
                this.curFrame = 0;
            }
        }

        if (GLFW.glfwGetKey(this.window, 75) == 1) {
            if (!this.skinSwitchHeld) {
                this.skinIndex = (this.skinIndex + 1) % this.skins.length;
                this.loadSkin(this.skinIndex);
                this.skinSwitchHeld = true;
            }
        } else {
            this.skinSwitchHeld = false;
        }
    }

    private void renderWorld() {
        this.currentYaw += (this.targetYaw - this.currentYaw) * 0.2f;
        this.currentPitch += (this.targetPitch - this.currentPitch) * 0.2f;
        GL11.glMatrixMode(5889);
        GL11.glLoadIdentity();
        float aspect = (float)this.winW / (float)this.winH;
        float fovSize = this.isFirstPerson ? 0.05f : 0.1f;
        GL11.glFrustum(-aspect * fovSize, aspect * fovSize, -fovSize, fovSize, 0.1f, 1000.0);
        GL11.glMatrixMode(5888);
        GL11.glLoadIdentity();
        if (this.isFirstPerson) {
            GL11.glTranslatef(0.0f, -1.6f, 0.0f);
        } else {
            GL11.glTranslatef(0.0f, -2.8f, -15.0f * this.zoomLevel);
        }
        GL11.glRotatef(this.currentPitch, 1.0f, 0.0f, 0.0f);
        GL11.glRotatef(this.currentYaw, 0.0f, 1.0f, 0.0f);
        GL11.glTranslatef(-this.pX, -this.pY, -this.pZ);
        GL11.glDisable(3553);
        GL11.glColor3f(0.24f, 0.42f, 0.24f);
        GL11.glBegin(7);
        GL11.glVertex3f(this.pX - 400.0f, 0.0f, this.pZ - 400.0f);
        GL11.glVertex3f(this.pX + 400.0f, 0.0f, this.pZ - 400.0f);
        GL11.glVertex3f(this.pX + 400.0f, 0.0f, this.pZ + 400.0f);
        GL11.glVertex3f(this.pX - 400.0f, 0.0f, this.pZ + 400.0f);
        GL11.glEnd();
        for (Tree tree : this.trees) {
            if (tree.chopped) continue;
            this.draw3DTree(tree.x, tree.z);
        }
        for (Projectile projectile : this.arrows) {
            GL11.glPushMatrix();
            GL11.glTranslatef(projectile.x, projectile.y, projectile.z);
            GL11.glRotatef(-this.currentYaw, 0.0f, 1.0f, 0.0f);
            GL11.glEnable(3553);
            GL11.glBindTexture(3553, this.arrowTex);
            this.drawQuad(1.0f);
            GL11.glPopMatrix();
        }
        for (Animal animal : this.animals) {
            if (!animal.alive) continue;
            GL11.glPushMatrix();
            GL11.glTranslatef(animal.x, animal.y + 1.0f, animal.z);
            GL11.glRotatef(-this.currentYaw, 0.0f, 1.0f, 0.0f);
            GL11.glEnable(3553);
            GL11.glBindTexture(3553, animal.type == 0 ? this.cowTex : this.pigTex);
            this.drawQuad(2.2f);
            GL11.glPopMatrix();
        }
        for (Zombie z : zombies) {
            if (!z.alive) continue;

            GL11.glPushMatrix();
            GL11.glTranslatef(z.x, z.y + 1.2f, z.z);
            GL11.glRotatef(-this.currentYaw, 0, 1, 0);

            GL11.glEnable(3553);
            GL11.glBindTexture(3553, z.getTex());

            this.drawQuad(2.3f);

            GL11.glPopMatrix();
        }
        if (!this.isFirstPerson) {
            this.drawPlayer();
        }
        GL11.glDisable(3553);
        GL11.glColor3f(0.2f, 0.8f, 1.0f);
        for (NetState.Player player : this.others) {
            GL11.glPushMatrix();
            GL11.glTranslatef(player.x, player.y + 1.25f, player.z);
            GL11.glRotatef(-this.currentYaw, 0.0f, 1.0f, 0.0f);
            this.drawQuad(2.5f);
            GL11.glPopMatrix();
        }
    }


    private void drawPlayer() {
        GL11.glPushMatrix();
        GL11.glTranslatef(this.pX, this.pY + 1.25f, this.pZ);
        GL11.glRotatef(-this.currentYaw, 0.0f, 1.0f, 0.0f);

        int tex = 0;

        if (this.walkFrames != null && this.walkFrames.length > 0) {
            tex = this.walkFrames[0];
        }

        if (this.isAttacking) {
            ItemStack cur = this.inventory[this.selectedSlot];

            if (cur != null && cur.type == 2) {
                if (this.walkFrames != null && this.walkFrames.length > 0) {
                    tex = this.walkFrames[0];
                }
            } else {
                if (this.attackFrames != null && this.attackFrames.length > 0) {
                    tex = this.attackFrames[this.curFrame % this.attackFrames.length];
                }
            }

        } else if (this.isMoving) {
            if (this.walkFrames != null && this.walkFrames.length > 0) {
                tex = this.walkFrames[this.curFrame % this.walkFrames.length];
            }
        }

        GL11.glEnable(3553);
        GL11.glBindTexture(3553, tex);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        this.drawQuad(2.5f);

        GL11.glPopMatrix();
    }


    private void renderUI() {
        GL11.glMatrixMode(5889);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0, this.winW, this.winH, 0.0, -1.0, 1.0);
        GL11.glMatrixMode(5888);
        GL11.glLoadIdentity();

        GL11.glDisable(2929);
        GL11.glDisable(3553);

        ItemStack cur = this.inventory[this.selectedSlot];

        // ===== Crosshair =====
        if (this.isFirstPerson) {
            GL11.glColor4f(1f, 1f, 1f, 0.9f);
            this.drawUIRect(this.winW / 2 - 1, this.winH / 2 - 10, 2, 20);
            this.drawUIRect(this.winW / 2 - 10, this.winH / 2 - 1, 20, 2);
        }

        // ===== Layout config (FIXED) =====
        float slotSize = 50f;
        float padding = 8f;

        float hotbarW = 5 * slotSize + 4 * padding;
        float startX = (this.winW - hotbarW) / 2f;
        float bottomY = this.winH - 90;

        // ===== Hotbar =====
        for (int i = 0; i < 5; i++) {
            float x = startX + i * (slotSize + padding);

            GL11.glDisable(3553);
            GL11.glColor4f(i == this.selectedSlot ? 1f : 0.1f, i == this.selectedSlot ? 1f : 0.1f, i == this.selectedSlot ? 1f : 0.1f, 0.6f);
            this.drawUIRect(x, bottomY, slotSize, slotSize);

            ItemStack s = this.inventory[i];

            if (s != null && s.texID != 0) {
                GL11.glEnable(3553);
                GL11.glBindTexture(3553, s.texID);
                GL11.glColor4f(1f, 1f, 1f, 1f);

                this.drawUITexturedRect(x + 6, bottomY + 6, slotSize - 12, slotSize - 12);

                if (s.count > 1) {
                    this.drawText(String.valueOf(s.count), x + slotSize - 18, bottomY + slotSize - 10, 1f);
                }
            }
        }

        // ===== Hearts (LEFT SIDE, OUTSIDE HOTBAR) =====
        float statsY = bottomY - 28;

        GL11.glEnable(3553);

        float heartStartX = startX - 180; // 争 pushed LEFT (no overlap)

        for (int i = 0; i < 10; i++) {
            int tex = 0;

            if (this.hp >= (i + 1) * 2) tex = this.heartFullTex;
            else if (this.hp >= i * 2 + 1) tex = this.heartHalfTex;

            if (tex != 0) {
                GL11.glBindTexture(3553, tex);
                this.drawUITexturedRect(heartStartX + i * 16, statsY, 14, 14);
            }
        }

        // ===== Hunger (RIGHT SIDE, OUTSIDE HOTBAR) =====
        float hungerStartX = startX + hotbarW + 20; // 争 pushed RIGHT

        for (int i = 0; i < 10; i++) {
            int tex = 0;

            if (this.hunger >= (i + 1) * 2) tex = this.hungFullTex;
            else if (this.hunger >= i * 2 + 1) tex = this.hungHalfTex;

            if (tex != 0) {
                GL11.glBindTexture(3553, tex);
                this.drawUITexturedRect(hungerStartX + i * 16, statsY, 14, 14);
            }
        }

        // ===== Text =====
        GL11.glDisable(3553);

        this.drawText("Skin: " + this.skins[this.skinIndex] + " (K)", 20, this.winH - 50, 1f);

        this.drawText(
                "Selected: " + (cur != null ? cur.name + " x" + cur.count : "Empty"),
                startX,
                bottomY - 50,
                1.1f
        );

        this.drawText(
                "1-5 | E | Shift | Space",
                20,
                this.winH - 20,
                1f
        );

        // ===== NATIVE IMAGING FOR THE GL-GEOMETRY MOBILE JOYSTICK HUD OVERLAYS =====
        if (this.isMobile) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);

            // 1. Render Outer Joystick Track Ring Frame (Drawn using a 24-point step vector circle loop)
            GL11.glColor4f(0.2f, 0.2f, 0.2f, 0.4f);
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glVertex2f(this.joystickCenterX, this.joystickCenterY);
            for (int angle = 0; angle <= 360; angle += 15) {
                float rads = (float) Math.toRadians(angle);
                GL11.glVertex2f(
                        this.joystickCenterX + (float) Math.cos(rads) * this.joystickRadius,
                        this.joystickCenterY + (float) Math.sin(rads) * this.joystickRadius
                );
            }
            GL11.glEnd();

            // 2. Render Inner Dynamic Tracking Knob Handle Thumb Ring
            float currentKnobVisualX = this.joystickCenterX + this.knobX * this.joystickRadius;
            float currentKnobVisualY = this.joystickCenterY + this.knobY * this.joystickRadius;

            GL11.glColor4f(0.8f, 0.8f, 0.8f, 0.7f);
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glVertex2f(currentKnobVisualX, currentKnobVisualY);
            for (int angle = 0; angle <= 360; angle += 15) {
                float rads = (float) Math.toRadians(angle);
                GL11.glVertex2f(
                        currentKnobVisualX + (float) Math.cos(rads) * 25.0f,
                        currentKnobVisualY + (float) Math.sin(rads) * 25.0f
                );
            }
            GL11.glEnd();

            // 3. Render Jump Action Button Layout Box
            if (this.isJumpPressed) {
                GL11.glColor4f(0.6f, 0.6f, 0.6f, 0.8f); // Active color highlights when pressed down
            } else {
                GL11.glColor4f(0.3f, 0.3f, 0.3f, 0.5f); // Base dark shade container layout
            }
            this.drawUIRect(this.jumpBtnX, this.jumpBtnY, this.jumpBtnW, this.jumpBtnH);
            this.drawText("JUMP", this.jumpBtnX + 32.0f, this.jumpBtnY + 22.0f, 1.0f);
        }

        if (this.damageFlash > 0f) {
            GL11.glDisable(3553);
            GL11.glColor4f(1f, 0f, 0f, this.damageFlash * 0.3f);

            // full screen overlay
            this.drawUIRect(0, 0, this.winW, this.winH);

            this.damageFlash *= 0.9f; // fade out
        }

        GL11.glEnable(2929);
    }

    private void loop() {
        while (!GLFW.glfwWindowShouldClose(this.window)) {
            if (this.showSplash) {
                this.renderSplash();
                GLFW.glfwSwapBuffers(this.window);
                GLFW.glfwPollEvents();
                if (GLFW.glfwGetMouseButton(this.window, 0) != 1 && GLFW.glfwGetKey(this.window, 32) != 1 && GLFW.glfwGetKey(this.window, 257) != 1) continue;
                this.showSplash = false;
                continue;
            }
            this.handleInput();
            this.updateSurvivalStats();
            this.updateAnims();
            this.updateProjectiles();
            for (Animal a : this.animals) {
                a.update();
            }
            for (Zombie z : zombies) {
                z.update(this.pX, this.pZ);

                if (!z.alive) continue;

                float dist = (float)Math.sqrt(
                        Math.pow(z.x - this.pX, 2) +
                                Math.pow(z.z - this.pZ, 2)
                );

                if (dist < 1.5f) {
                    this.hp -= 0.02f;      // damage over time
                    this.damageFlash = 1f; // trigger red flash
                }
            }
            long now = System.currentTimeMillis();
            if (now - this.lastNetSync > 200L) {
                NetState.upsert(this.playerName, this.pX, this.pY, this.pZ, this.currentYaw, this.currentPitch);
                this.others = NetState.others(this.playerName, 1500L);
                this.lastNetSync = now;
            }
            GL11.glClear(16640);
            this.renderWorld();
            this.renderUI();
            GLFW.glfwSwapBuffers(this.window);
            GLFW.glfwPollEvents();
        }
    }

    private void updateSurvivalStats() {
        long now = System.currentTimeMillis();
        if (now - this.lastTickTime > 1000L) {
            this.hunger = Math.max(0.0f, this.hunger - 0.1f);
            if (this.hunger <= 0.0f) {
                this.hp = Math.max(0.0f, this.hp - 0.5f);
            } else if (this.hunger > 17.0f && this.hp < 20.0f) {
                this.hp = Math.min(20.0f, this.hp + 0.5f);
            }
            this.lastTickTime = now;
        }
        if (!this.isMoving) {
            this.stamina = Math.min(100.0f, this.stamina + 1.5f);
        }
    }

    private void updateAnims() {
        long now = System.currentTimeMillis();
        if (now - this.lastAnimTime > (long)(this.isAttacking ? 70 : 140)) {
            ++this.curFrame;
            if (this.isAttacking && this.curFrame >= this.attackFrames.length) {
                this.isAttacking = false;
                this.curFrame = 0;
            } else {
                this.curFrame = this.isMoving ? (this.curFrame %= this.walkFrames.length) : 0;
            }
            this.lastAnimTime = now;
        }
    }

    private void updateProjectiles() {
        Iterator<Projectile> it = this.arrows.iterator();
        block0: while (it.hasNext()) {
            Projectile a = it.next();
            a.update();
            if (a.y < -5.0f) {
                it.remove();
                continue;
            }
            for (Animal animal : this.animals) {
                if (!animal.alive || !(Math.sqrt(Math.pow(a.x - animal.x, 2.0) + Math.pow(a.z - animal.z, 2.0)) < 1.5)) continue;
                animal.alive = false;
                this.addItemToInv("Meat", this.meatTex, 2, 1);
                this.xp += 30.0f;
                it.remove();
                continue block0;
            }
            for (Zombie z : zombies) {
                if (!z.alive) continue;

                if (Math.sqrt(Math.pow(a.x - z.x, 2) + Math.pow(a.z - z.z, 2)) < 1.5) {
                    z.hp -= 6f;

                    if (z.hp <= 0) {
                        z.alive = false;
                        this.xp += 40f;
                    }

                    it.remove();
                    continue block0;
                }
            }
        }
    }

    private void checkCombatCollision() {
        float rad = (float)Math.toRadians(this.currentYaw);
        float hX = this.pX + (float)Math.sin(rad) * 3.5f;
        float hZ = this.pZ - (float)Math.cos(rad) * 3.5f;
        for (Animal a : this.animals) {
            if (!a.alive || !(Math.sqrt(Math.pow(hX - a.x, 2.0) + Math.pow(hZ - a.z, 2.0)) < 2.5)) continue;
            a.alive = false;
            this.addItemToInv("Meat", this.meatTex, 2, 1);
            this.xp += 20.0f;
        }
        for (Tree t : this.trees) {
            if (t.chopped || !(Math.sqrt(Math.pow(hX - t.x, 2.0) + Math.pow(hZ - t.z, 2.0)) < 2.5)) continue;
            t.chopped = true;
            this.addItemToInv("Log", this.logTex, 3, 2);
            this.xp += 10.0f;
        }
        for (Zombie z : zombies) {
            if (!z.alive) continue;

            if (Math.sqrt(Math.pow(hX - z.x, 2) + Math.pow(hZ - z.z, 2)) < 2.5) {
                z.hp -= 6f; // sword damage

                if (z.hp <= 0) {
                    z.alive = false;
                    this.xp += 40f;
                }
            }
        }
    }

    private void addItemToInv(String n, int t, int ty, int amt) {
        for (ItemStack is : this.inventory) {
            if (is == null || !is.name.equals(n) || is.count >= 64) continue;
            is.count = Math.min(64, is.count + amt);
            return;
        }
        for (int i = 0; i < 5; ++i) {
            if (this.inventory[i] != null) continue;
            this.inventory[i] = new ItemStack(n, t, ty, amt);
            return;
        }
    }

    private boolean consumeArrow() {
        return true;
    }

    private int countItems(int type) {
        int c = 0;
        for (ItemStack s : this.inventory) {
            if (s == null || s.type != type) continue;
            c += s.count;
        }
        return c;
    }

    private void draw3DTree(float tx, float tz) {
        GL11.glPushMatrix();
        GL11.glTranslatef(tx, 0.0f, tz);
        GL11.glEnable(3553);
        GL11.glBindTexture(3553, this.woodTex);
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        this.drawTexturedBox(0.6f, 3.5f, 0.6f);
        GL11.glTranslatef(0.0f, 4.5f, 0.0f);
        GL11.glBindTexture(3553, this.grassTex);
        this.drawTexturedBox(2.5f, 2.2f, 2.5f);
        GL11.glPopMatrix();
    }

    private void drawTexturedBox(float x, float y, float z) {
        GL11.glBegin(7);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex3f(-x, y, z);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex3f(x, y, z);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex3f(x, -y, z);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex3f(-x, -y, z);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex3f(-x, y, -z);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex3f(x, y, -z);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex3f(x, -y, -z);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex3f(-x, -y, -z);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex3f(-x, y, -z);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex3f(x, y, -z);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex3f(x, y, z);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex3f(-x, y, z);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex3f(-x, -y, -z);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex3f(x, -y, -z);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex3f(x, -y, z);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex3f(-x, -y, z);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex3f(x, y, -z);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex3f(x, y, z);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex3f(x, -y, z);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex3f(x, -y, -z);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex3f(-x, y, -z);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex3f(-x, y, z);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex3f(-x, -y, z);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex3f(-x, -y, -z);
        GL11.glEnd();
    }

    private void drawQuad(float s) {
        GL11.glBegin(7);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex3f(-s / 2.0f, s / 2.0f, 0.0f);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex3f(s / 2.0f, s / 2.0f, 0.0f);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex3f(s / 2.0f, -s / 2.0f, 0.0f);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex3f(-s / 2.0f, -s / 2.0f, 0.0f);
        GL11.glEnd();
    }

    private void drawUIRect(float x, float y, float w, float h) {
        GL11.glBegin(7);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + w, y);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x, y + h);
        GL11.glEnd();
    }

    private void drawUITexturedRect(float x, float y, float w, float h) {
        GL11.glBegin(7);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex2f(x + w, y);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex2f(x + w, y + h);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex2f(x, y + h);
        GL11.glEnd();
    }

    private int loadTexture(String path) {
        try (InputStream is = Main.class.getResourceAsStream("/" + path);){
            ByteBuffer img;
            IntBuffer h;
            IntBuffer w;
            MemoryStack s;
            block20: {
                if (is == null) {
                    System.out.println("Missing texture: " + path);
                    int n = 0;
                    return n;
                }
                byte[] bytes = is.readAllBytes();
                ByteBuffer buf = BufferUtils.createByteBuffer(bytes.length).put(bytes).flip();
                s = MemoryStack.stackPush();
                try {
                    w = s.mallocInt(1);
                    h = s.mallocInt(1);
                    IntBuffer c = s.mallocInt(1);
                    img = STBImage.stbi_load_from_memory(buf, w, h, c, 4);
                    if (img != null) break block20;
                    System.out.println("Failed to decode texture: " + path);
                    int n = 0;
                    if (s != null) {
                        s.close();
                    }
                    return n;
                }
                catch (Throwable throwable) {
                    if (s != null) {
                        try {
                            s.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
            }
            int id = GL11.glGenTextures();
            GL11.glBindTexture(3553, id);
            GL11.glTexParameterf(3553, 10241, 9728.0f);
            GL11.glTexParameterf(3553, 10240, 9728.0f);
            GL11.glTexImage2D(3553, 0, 6408, w.get(), h.get(), 0, 6408, 5121, img);
            STBImage.stbi_image_free(img);
            int n = id;
            if (s != null) {
                s.close();
            }
            return n;
        }
        catch (Exception e) {
            return 0;
        }
    }

    private void drawText(String text, float x, float y, float scale) {
        if (text == null || text.isEmpty()) return;

        ByteBuffer buffer = BufferUtils.createByteBuffer(text.length() * 270);
        int quads = STBEasyFont.stb_easy_font_print(0, 0, text, null, buffer);

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(scale * 1.2f, scale * 1.2f, 1f);

        GL11.glDisable(3553);

        // ===== THICK SHADOW (multi-pass) =====
        GL11.glColor3f(0f, 0f, 0f);
        for (int ox = -1; ox <= 1; ox++) {
            for (int oy = -1; oy <= 1; oy++) {
                if (ox == 0 && oy == 0) continue;

                GL11.glPushMatrix();
                GL11.glTranslatef(ox, oy, 0);

                GL11.glEnableClientState(32884);
                GL11.glVertexPointer(2, 5126, 16, buffer);
                GL11.glDrawArrays(7, 0, quads * 4);
                GL11.glDisableClientState(32884);

                GL11.glPopMatrix();
            }
        }

        // ===== MAIN TEXT =====
        GL11.glColor3f(1f, 1f, 1f);
        GL11.glEnableClientState(32884);
        GL11.glVertexPointer(2, 5126, 16, buffer);
        GL11.glDrawArrays(7, 0, quads * 4);
        GL11.glDisableClientState(32884);

        GL11.glPopMatrix();
    }

    private void loadSkin(int idx) {
        int i;
        String base = this.skins[idx];
        for (int i2 = 0; i2 < this.walkFrames.length; ++i2) {
            this.walkFrames[i2] = this.loadTexture(base + "/" + (i2 + 1) + ".gif");
        }
        boolean swordLoaded = true;
        for (i = 0; i < 4; ++i) {
            this.attackFrames[i] = this.loadTexture(base + "/sword (" + (i + 1) + ").png");
            if (this.attackFrames[i] != 0) continue;
            swordLoaded = false;
        }
        if (!swordLoaded || "Blue".equalsIgnoreCase(base)) {
            System.arraycopy(this.defaultAttackFrames, 0, this.attackFrames, 0, 4);
        }
        for (i = 0; i < this.walkFrames.length; ++i) {
            if (this.walkFrames[i] != 0) continue;
            this.walkFrames[i] = this.walkFrames[0];
        }
        if (this.walkFrames[0] == 0) {
            for (i = 0; i < this.walkFrames.length; ++i) {
                this.walkFrames[i] = this.loadTexture("frame" + (i + 1) + ".png");
            }
        }
    }

    private void renderSplash() {
        GL11.glDisable(2929);
        GL11.glClear(16384);
        GL11.glMatrixMode(5889);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0, this.winW, this.winH, 0.0, -1.0, 1.0);
        GL11.glMatrixMode(5888);
        GL11.glLoadIdentity();
        GL11.glEnable(3553);
        GL11.glBindTexture(3553, this.splashTex);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glBegin(7);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex2f(0.0f, 0.0f);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex2f(this.winW, 0.0f);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex2f(this.winW, this.winH);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex2f(0.0f, this.winH);
        GL11.glEnd();
        GL11.glEnable(2929);
    }

    private void initDiscord() {
        DiscordEventHandlers h = new DiscordEventHandlers.Builder().build();
        DiscordRPC.discordInitialize(DISCORD_ID, h, true);
        DiscordRPC.discordUpdatePresence(new DiscordRichPresence.Builder("Survival").setBigImage("logo", "TimmiLand").build());
    }

    private void cleanup() {
        DiscordRPC.discordShutdown();
        GLFW.glfwDestroyWindow(this.window);
        GLFW.glfwTerminate();
    }

    public static void main(String[] args) {
        new Main().run();
    }

    class ItemStack {
        String name;
        int count;
        int texID;
        int type;

        ItemStack(String n, int tID, int ty, int c) {
            this.name = n;
            this.texID = tID;
            this.type = ty;
            this.count = c;
        }
    }

    class Animal {
        float x;
        float z;
        float y;
        int type;
        boolean alive = true;
        float tX;
        float tZ;
        long lastMove;

        Animal(float x, float z, int type) {
            this.x = x;
            this.z = z;
            this.type = type;
            this.tX = x;
            this.tZ = z;
        }

        void update() {
            if (!this.alive) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now - this.lastMove > 3000L) {
                this.tX = this.x + (new Random().nextFloat() - 0.5f) * 20.0f;
                this.tZ = this.z + (new Random().nextFloat() - 0.5f) * 20.0f;
                this.lastMove = now;
            }
            this.x += (this.tX - this.x) * 0.015f;
            this.z += (this.tZ - this.z) * 0.015f;
            this.y = (float)Math.abs(Math.sin((float)now * 0.008f)) * 0.2f;
        }
    }

    class Tree {
        float x;
        float z;
        boolean chopped = false;

        Tree(float x, float z) {
            this.x = x;
            this.z = z;
        }
    }

    class Projectile {
        float x;
        float y;
        float z;
        float vx;
        float vy;
        float vz;

        Projectile(float x, float y, float z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            float rYaw = (float)Math.toRadians(yaw);
            float rPitch = (float)Math.toRadians(pitch);
            float speed = 0.8f;
            this.vx = (float)(Math.sin(rYaw) * Math.cos(rPitch)) * speed;
            this.vy = (float)(-Math.sin(rPitch)) * speed;
            this.vz = (float)(-Math.cos(rYaw) * Math.cos(rPitch)) * speed;
        }

        void update() {
            this.x += this.vx;
            this.y += this.vy;
            this.z += this.vz;
            this.vy -= 0.005f;
        }
    }

    class Zombie {
        float x, y, z;
        float hp = 30f;
        boolean alive = true;

        int[] frames = new int[4];
        int frame = 0;
        long lastAnim = 0;

        Zombie(float x, float z) {
            this.x = x;
            this.z = z;

            for (int i = 0; i < 4; i++) {
                frames[i] = loadTexture("zombie-" + (i + 1) + ".png");
            }
        }

        void update(float playerX, float playerZ) {
            if (!alive) return;

            float dx = playerX - x;
            float dz = playerZ - z;
            float dist = (float)Math.sqrt(dx * dx + dz * dz);

            if (dist > 1.5f) {
                x += dx * 0.01f;
                z += dz * 0.01f;
            }

            long now = System.currentTimeMillis();
            if (now - lastAnim > 150) {
                frame = (frame + 1) % frames.length;
                lastAnim = now;
            }
        }

        int getTex() {
            return frames[frame];
        }
    }
}
