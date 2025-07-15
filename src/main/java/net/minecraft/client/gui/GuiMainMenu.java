package net.minecraft.client.gui;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.src.Config;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.demo.DemoWorldServer;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.WorldInfo;
import net.optifine.CustomPanorama;
import net.optifine.CustomPanoramaProperties;
import org.apache.commons.io.Charsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjglx.opengl.GLContext;
import org.lwjglx.util.glu.Project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class GuiMainMenu extends GuiScreen implements GuiYesNoCallback {
    private static final Logger logger = LogManager.getLogger();
    private static final Random RANDOM = new Random();
    private static final String DEMO_WORLD_NAME = "Demo_World";
    private static final String COPYRIGHT_TEXT = "Copyright Mojang AB. Do not distribute!";
    private static final String CLIENT_COPYRIGHT_TEXT = "Copyright FoxMCTeam. All Rights Reserved";
    private static final int PANORAMA_SIDES = 6;
    private static final int TITLE_WIDTH = 274;
    private static final int TITLE_HEIGHT = 44;
    private static final int TITLE_Y_OFFSET = 30;
    private static final ResourceLocation splashTexts = new ResourceLocation("texts/splashes.txt");
    private static final ResourceLocation minecraftTitleTextures = new ResourceLocation("textures/gui/title/minecraft.png");
    private static final ResourceLocation[] titlePanoramaPaths = new ResourceLocation[] {
            new ResourceLocation("textures/gui/title/background/panorama_0.png"),
            new ResourceLocation("textures/gui/title/background/panorama_1.png"),
            new ResourceLocation("textures/gui/title/background/panorama_2.png"),
            new ResourceLocation("textures/gui/title/background/panorama_3.png"),
            new ResourceLocation("textures/gui/title/background/panorama_4.png"),
            new ResourceLocation("textures/gui/title/background/panorama_5.png")
    };
    private final float updateCounter;
    private String splashText = "miss";
    private int panoramaTimer;
    private final Object threadLock = new Object();
    private String openGLWarning1 = "";
    private String openGLWarning2 = "Please click " + EnumChatFormatting.UNDERLINE + "here" + EnumChatFormatting.RESET + " for more information.";
    private String openGLWarningLink;
    private int field_92024_r;
    private int field_92023_s;
    private int field_92022_t;
    private int field_92021_u;
    private int field_92020_v;
    private int field_92019_w;
    private ResourceLocation backgroundTexture;

    public GuiMainMenu() {
        loadSplashTexts();
        checkOpenGLCapabilities();
        this.updateCounter = RANDOM.nextFloat();
    }

    private void loadSplashTexts() {
        BufferedReader reader = null;
        try {
            List<String> splashList = Lists.newArrayList();
            reader = new BufferedReader(new InputStreamReader(
                    Minecraft.getMinecraft().getResourceManager().getResource(splashTexts).getInputStream(),
                    Charsets.UTF_8
            ));

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    splashList.add(line);
                }
            }

            if (!splashList.isEmpty()) {
                selectRandomSplash(splashList);
            }
        } catch (IOException ex) {
            logger.error("Failed to load splash texts", ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    logger.error("Failed to close splash text reader", ex);
                }
            }
        }
    }

    private void selectRandomSplash(List<String> splashList) {
        do {
            this.splashText = splashList.get(RANDOM.nextInt(splashList.size()));
        } while (this.splashText.hashCode() == 125780783);
    }

    private void checkOpenGLCapabilities() {
        if (!GLContext.getCapabilities().OpenGL20 && !OpenGlHelper.areShadersSupported()) {
            this.openGLWarning1 = I18n.format("title.oldgl1");
            this.openGLWarning2 = I18n.format("title.oldgl2");
            this.openGLWarningLink = "https://help.mojang.com/customer/portal/articles/325948?ref=game";
        }
    }

    @Override
    public void updateScreen() {
        ++this.panoramaTimer;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void initGui() {
        initBackgroundTexture();
        checkSpecialDates();
        initMenuButtons();
        initOpenGLWarningBounds();
        this.mc.func_181537_a(false);
    }

    private void initBackgroundTexture() {
        DynamicTexture viewportTexture = new DynamicTexture(256, 256);
        this.backgroundTexture = this.mc.getTextureManager().getDynamicTextureLocation("background", viewportTexture);
    }

    private void checkSpecialDates() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        if (month == 12 && day == 24) {
            this.splashText = "Merry X-mas!";
        } else if (month == 1 && day == 1) {
            this.splashText = "Happy new year!";
        } else if (month == 10 && day == 31) {
            this.splashText = "OOoooOOOoooo! Spooky!";
        }
    }

    private void initMenuButtons() {
        int buttonY = this.height / 4 + 48;
        int buttonSpacing = 24;

        if (this.mc.isDemo()) {
            addDemoButtons(buttonY, buttonSpacing);
        } else {
            addSingleplayerMultiplayerButtons(buttonY, buttonSpacing);
        }

        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, buttonY + 72 + 12, 98, 20, I18n.format("menu.options")));
        this.buttonList.add(new GuiButton(4, this.width / 2 + 2, buttonY + 72 + 12, 98, 20, I18n.format("menu.quit")));
        this.buttonList.add(new GuiButtonLanguage(5, this.width / 2 - 124, buttonY + 72 + 12));
    }

    private void addSingleplayerMultiplayerButtons(int yPos, int spacing) {
        this.buttonList.add(new GuiButton(1, this.width / 2 - 100, yPos, I18n.format("menu.singleplayer")));
        this.buttonList.add(new GuiButton(2, this.width / 2 - 100, yPos + spacing, I18n.format("menu.multiplayer")));
    }

    private void addDemoButtons(int yPos, int spacing) {
        this.buttonList.add(new GuiButton(11, this.width / 2 - 100, yPos, I18n.format("menu.playdemo")));
        GuiButton buttonResetDemo;
        this.buttonList.add(buttonResetDemo = new GuiButton(12, this.width / 2 - 100, yPos + spacing, I18n.format("menu.resetdemo")));

        ISaveFormat saveLoader = this.mc.getSaveLoader();
        WorldInfo worldInfo = saveLoader.getWorldInfo(DEMO_WORLD_NAME);
        buttonResetDemo.enabled = worldInfo != null;
    }

    private void initOpenGLWarningBounds() {
        synchronized (this.threadLock) {
            this.field_92023_s = this.fontRendererObj.getStringWidthInt(this.openGLWarning1);
            this.field_92024_r = this.fontRendererObj.getStringWidthInt(this.openGLWarning2);
            int maxWidth = Math.max(this.field_92023_s, this.field_92024_r);
            this.field_92022_t = (this.width - maxWidth) / 2;
            this.field_92021_u = this.buttonList.get(0).yPosition - 24;
            this.field_92020_v = this.field_92022_t + maxWidth;
            this.field_92019_w = this.field_92021_u + 24;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:
                this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
                break;
            case 1:
                this.mc.displayGuiScreen(new GuiSelectWorld(this));
                break;
            case 2:
                this.mc.displayGuiScreen(new GuiMultiplayer(this));
                break;
            case 4:
                this.mc.shutdown();
                break;
            case 5:
                this.mc.displayGuiScreen(new GuiLanguage(this, this.mc.gameSettings, this.mc.getLanguageManager()));
                break;
            case 11:
                this.mc.launchIntegratedServer(DEMO_WORLD_NAME, DEMO_WORLD_NAME, DemoWorldServer.demoWorldSettings);
                break;
            case 12:
                ISaveFormat saveLoader = this.mc.getSaveLoader();
                WorldInfo worldInfo = saveLoader.getWorldInfo(DEMO_WORLD_NAME);
                if (worldInfo != null) {
                    GuiYesNo confirmScreen = GuiSelectWorld.func_152129_a(this, worldInfo.getWorldName(), 12);
                    this.mc.displayGuiScreen(confirmScreen);
                }
                break;
        }
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (result) {
            switch (id) {
                case 12:
                    ISaveFormat saveLoader = this.mc.getSaveLoader();
                    saveLoader.flushCache();
                    saveLoader.deleteWorldDirectory(DEMO_WORLD_NAME);
                    break;
                case 13:
                    openGLWarningLink();
                    break;
            }
        }
        this.mc.displayGuiScreen(this);
    }

    private void openGLWarningLink() {
        try {
            Class<?> desktopClass = Class.forName("java.awt.Desktop");
            Object desktop = desktopClass.getMethod("getDesktop").invoke(null);
            desktopClass.getMethod("browse", URI.class).invoke(desktop, new URI(this.openGLWarningLink));
        } catch (Throwable ex) {
            logger.error("Couldn't open link", ex);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GlStateManager.disableAlpha();
        renderSkybox(partialTicks);
        GlStateManager.enableAlpha();

        renderOverlayGradients();
        renderTitle();
        renderSplashText();
        renderVersionInfo();
        renderCopyright();
        renderOpenGLWarning();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void renderSkybox(float partialTicks) {
        this.mc.getFramebuffer().unbindFramebuffer();
        GlStateManager.viewport(0, 0, 256, 256);
        drawPanorama(partialTicks);
        rotateAndBlurSkybox();

        CustomPanoramaProperties panoramaProps = CustomPanorama.getCustomPanoramaProperties();
        int blurPasses = panoramaProps != null ? panoramaProps.getBlur3() : 3;

        for (int i = 0; i < blurPasses; ++i) {
            rotateAndBlurSkybox();
            rotateAndBlurSkybox();
        }

        this.mc.getFramebuffer().bindFramebuffer(true);
        GlStateManager.viewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);

        float aspectRatio = this.width > this.height ? 120.0F / this.width : 120.0F / this.height;
        float heightScale = this.height * aspectRatio / 256.0F;
        float widthScale = this.width * aspectRatio / 256.0F;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        worldrenderer.pos(0.0D, this.height, this.zLevel).tex(0.5F - heightScale, 0.5F + widthScale).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        worldrenderer.pos(this.width, this.height, this.zLevel).tex(0.5F - heightScale, 0.5F - widthScale).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        worldrenderer.pos(this.width, 0.0D, this.zLevel).tex(0.5F + heightScale, 0.5F - widthScale).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        worldrenderer.pos(0.0D, 0.0D, this.zLevel).tex(0.5F + heightScale, 0.5F + widthScale).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        tessellator.draw();
    }

    private void drawPanorama(float partialTicks) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        Project.gluPerspective(120.0F, 1.0F, 0.05F, 10.0F);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        CustomPanoramaProperties panoramaProps = CustomPanorama.getCustomPanoramaProperties();
        int blur1 = panoramaProps != null ? panoramaProps.getBlur1() : 64;

        for (int i = 0; i < blur1; ++i) {
            GlStateManager.pushMatrix();
            float offsetX = ((i % 8F) / 8.0F - 0.5F) / 64.0F;
            float offsetY = ((i / 8F) / 8.0F - 0.5F) / 64.0F;
            GlStateManager.translate(offsetX, offsetY, 0.0F);

            float rotationX = MathHelper.sin((this.panoramaTimer + partialTicks) / 400.0F) * 25.0F + 20.0F;
            float rotationY = -(this.panoramaTimer + partialTicks) * 0.1F;
            GlStateManager.rotate(rotationX, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(rotationY, 0.0F, 1.0F, 0.0F);

            for (int side = 0; side < PANORAMA_SIDES; ++side) {
                renderPanoramaSide(worldrenderer, side, i, panoramaProps);
            }

            GlStateManager.popMatrix();
            GlStateManager.colorMask(true, true, true, false);
        }

        worldrenderer.setTranslation(0.0D, 0.0D, 0.0D);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
    }

    private void renderPanoramaSide(WorldRenderer worldrenderer, int side, int blurPass, CustomPanoramaProperties props) {
        GlStateManager.pushMatrix();

        switch (side) {
            case 1: GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F); break;
            case 2: GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F); break;
            case 3: GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F); break;
            case 4: GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F); break;
            case 5: GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F); break;
        }

        ResourceLocation[] panoramaLocations = props != null ? props.getPanoramaLocations() : titlePanoramaPaths;
        this.mc.getTextureManager().bindTexture(panoramaLocations[side]);

        int alpha = 255 / (blurPass + 1);
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        worldrenderer.pos(-1.0D, -1.0D, 1.0D).tex(0.0D, 0.0D).color(255, 255, 255, alpha).endVertex();
        worldrenderer.pos(1.0D, -1.0D, 1.0D).tex(1.0D, 0.0D).color(255, 255, 255, alpha).endVertex();
        worldrenderer.pos(1.0D, 1.0D, 1.0D).tex(1.0D, 1.0D).color(255, 255, 255, alpha).endVertex();
        worldrenderer.pos(-1.0D, 1.0D, 1.0D).tex(0.0D, 1.0D).color(255, 255, 255, alpha).endVertex();
        Tessellator.getInstance().draw();

        GlStateManager.popMatrix();
    }

    /**
     * 旋转和模糊天空盒
     */
    private void rotateAndBlurSkybox() {
        this.mc.getTextureManager().bindTexture(this.backgroundTexture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, 256, 256);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.colorMask(true, true, true, false);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        GlStateManager.disableAlpha();

        CustomPanoramaProperties props = CustomPanorama.getCustomPanoramaProperties();
        int blurPasses = props != null ? props.getBlur2() : 3;

        for (int i = 0; i < blurPasses; ++i) {
            float alpha = 1.0F / (i + 1);
            float offset = (i - blurPasses / 2F) / 256.0F;

            worldrenderer.pos(this.width, this.height, this.zLevel).tex(0.0F + offset, 1.0D).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
            worldrenderer.pos(this.width, 0.0D, this.zLevel).tex(1.0F + offset, 1.0D).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
            worldrenderer.pos(0.0D, 0.0D, this.zLevel).tex(1.0F + offset, 0.0D).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
            worldrenderer.pos(0.0D, this.height, this.zLevel).tex(0.0F + offset, 0.0D).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
        }

        tessellator.draw();
        GlStateManager.enableAlpha();
        GlStateManager.colorMask(true, true, true, true);
    }

    /**
     * 渲染覆盖渐变
     */
    private void renderOverlayGradients() {
        CustomPanoramaProperties props = CustomPanorama.getCustomPanoramaProperties();
        int overlay1Top = props != null ? props.getOverlay1Top() : -2130706433;
        int overlay1Bottom = props != null ? props.getOverlay1Bottom() : 16777215;
        int overlay2Top = props != null ? props.getOverlay2Top() : 0;
        int overlay2Bottom = props != null ? props.getOverlay2Bottom() : Integer.MIN_VALUE;

        if (overlay1Top != 0 || overlay1Bottom != 0) {
            this.drawGradientRect(0, 0, this.width, this.height, overlay1Top, overlay1Bottom);
        }

        if (overlay2Top != 0 || overlay2Bottom != 0) {
            this.drawGradientRect(0, 0, this.width, this.height, overlay2Top, overlay2Bottom);
        }
    }

    /**
     * 渲染Minecraft标题
     */
    private void renderTitle() {
        this.mc.getTextureManager().bindTexture(minecraftTitleTextures);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        int titleX = this.width / 2 - TITLE_WIDTH / 2;
        int titleY = TITLE_Y_OFFSET;

        if ((double)this.updateCounter < 1.0E-4D) {
            this.drawTexturedModalRect(titleX, titleY, 0, 0, 99, TITLE_HEIGHT);
            this.drawTexturedModalRect(titleX + 99F, titleY, 129, 0, 27, TITLE_HEIGHT);
            this.drawTexturedModalRect(titleX + 99F + 26F, titleY, 126, 0, 3, TITLE_HEIGHT);
            this.drawTexturedModalRect(titleX + 99F + 26F + 3, titleY, 99, 0, 26, TITLE_HEIGHT);
            this.drawTexturedModalRect(titleX + 155F, titleY, 0, 45, 155, TITLE_HEIGHT);
        } else {
            this.drawTexturedModalRect(titleX, titleY, 0, 0, 155, TITLE_HEIGHT);
            this.drawTexturedModalRect(titleX + 155, titleY, 0, 45, 155, TITLE_HEIGHT);
        }
    }

    private void renderSplashText() {
        GlStateManager.pushMatrix();
        GlStateManager.translate(this.width / 2F + 90F, 70.0F, 0.0F);
        GlStateManager.rotate(-20.0F, 0.0F, 0.0F, 1.0F);

        float scale = 1.8F - MathHelper.abs(MathHelper.sin(Minecraft.getSystemTime() % 1000L / 1000.0F * (float)Math.PI * 2.0F) * 0.1F);
        scale = scale * 100.0F / (this.fontRendererObj.getStringWidthInt(this.splashText) + 32);
        GlStateManager.scale(scale, scale, scale);

        this.drawCenteredString(this.fontRendererObj, this.splashText, 0, -8, -256);
        GlStateManager.popMatrix();
    }

    private void renderVersionInfo() {
        String version = "Minecraft 1.8.9";
        String OFVersion = Config.getVersion();

        if (this.mc.isDemo()) {
            version += " Demo";
        }

        this.drawString(this.fontRendererObj, version, 2, this.height - 10 - 10, -1);
        this.drawString(this.fontRendererObj, OFVersion, 2, this.height - 10, -1);
    }

    private void renderCopyright() {
        this.drawString(this.fontRendererObj, COPYRIGHT_TEXT,
                this.width - this.fontRendererObj.getStringWidthInt(COPYRIGHT_TEXT) - 2,
                this.height - 10 - 10, -1);
        this.drawString(this.fontRendererObj, CLIENT_COPYRIGHT_TEXT,
                this.width - this.fontRendererObj.getStringWidthInt(CLIENT_COPYRIGHT_TEXT) - 2,
                this.height - 10, -1);
    }

    private void renderOpenGLWarning() {
        if (this.openGLWarning1 != null && !this.openGLWarning1.isEmpty()) {
            drawRect(this.field_92022_t - 2, this.field_92021_u - 2,
                    this.field_92020_v + 2, this.field_92019_w - 1, 1428160512);
            this.drawString(this.fontRendererObj, this.openGLWarning1,
                    this.field_92022_t, this.field_92021_u, -1);
            this.drawString(this.fontRendererObj, this.openGLWarning2,
                    (this.width - this.field_92024_r) / 2,
                    this.buttonList.get(0).yPosition - 12, -1);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        synchronized (this.threadLock) {
            if (!this.openGLWarning1.isEmpty() &&
                    mouseX >= this.field_92022_t && mouseX <= this.field_92020_v &&
                    mouseY >= this.field_92021_u && mouseY <= this.field_92019_w) {
                GuiConfirmOpenLink confirmScreen = new GuiConfirmOpenLink(this, this.openGLWarningLink, 13, true);
                confirmScreen.disableSecurityWarning();
                this.mc.displayGuiScreen(confirmScreen);
            }
        }
    }
}