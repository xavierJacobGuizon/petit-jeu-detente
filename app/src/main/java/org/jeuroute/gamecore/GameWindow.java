package org.jeuroute.gamecore;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.IntBuffer;
import java.util.Objects;
import org.jeuroute.gamecore.controllers.GameInputController;
import org.lwjgl.system.MemoryStack;

public class GameWindow {

	private int width;
	private int height;
	private int framebufferWidth;
	private int framebufferHeight;
	private final String title;
	private long window;
	private GameInputController inputController;

	public GameWindow(int width, int height, String title) {
		this.width = width;
		this.height = height;
		framebufferWidth = width;
		framebufferHeight = height;
		this.title = title;
	}

	public WindowMetrics getMetrics() {
		return new WindowMetrics(width, height, framebufferWidth, framebufferHeight);
	}

	public void setInputController(GameInputController inputController) {
		this.inputController = Objects.requireNonNull(inputController);
	}

	/**
	 * Initialise la fenêtre GLFW et configure le contexte OpenGL.
	 */
	public void init() {
		if (!glfwInit()) {
			throw new IllegalStateException("Unable to initialize GLFW");
		}

		// Configuration de GLFW
		glfwDefaultWindowHints();
		// Configure la fenêtre GLFW avant de la créer
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		// Autoriser la fenêtre à être redimensionnable
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

		// Crée la fenêtre GLFW avec les dimensions et le titre spécifiés
		window = glfwCreateWindow(width, height, title != null ? title : "Toto", NULL, NULL);
		if (window == NULL) {
			throw new RuntimeException("Failed to create the GLFW window");
		}
		glfwSetWindowSizeCallback(window, (win, newWidth, newHeight) -> {
			width = newWidth;
			height = newHeight;
			if (inputController != null) {
				inputController.onWindowMetricsChanged(getMetrics());
			}
		});
		glfwSetFramebufferSizeCallback(window, (win, newWidth, newHeight) -> {
			framebufferWidth = newWidth;
			framebufferHeight = newHeight;
		});

		// Configure les callbacks pour gérer les événements d'entrée
		glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
				glfwSetWindowShouldClose(win, true);
				return;
			}
			if (inputController != null) {
				inputController.handleKey(key, action);
			}
		});

		// Configure le callback pour suivre la position de la souris
		glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
			if (inputController != null) {
				inputController.handleCursorPosition(xpos, ypos);
			}
		});

		glfwSetScrollCallback(window, (win, xoffset, yoffset) -> {
			if (inputController != null) {
				inputController.handleScroll(yoffset);
			}
		});

		// Configure le callback pour gérer les boutons de la souris.
		glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
			if (inputController != null) {
				inputController.handleMouseButton(button, action);
			}
		});

		// Configure le contexte OpenGL pour la fenêtre GLFW
		glfwMakeContextCurrent(window);
		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer windowWidth = stack.mallocInt(1);
			IntBuffer windowHeight = stack.mallocInt(1);
			glfwGetWindowSize(window, windowWidth, windowHeight);
			width = windowWidth.get(0);
			height = windowHeight.get(0);
			IntBuffer framebufferWidthBuffer = stack.mallocInt(1);
			IntBuffer framebufferHeightBuffer = stack.mallocInt(1);
			glfwGetFramebufferSize(window, framebufferWidthBuffer, framebufferHeightBuffer);
			framebufferWidth = framebufferWidthBuffer.get(0);
			framebufferHeight = framebufferHeightBuffer.get(0);
		}
		// Active le v-sync
		glfwSwapInterval(1);
		// Affiche la fenêtre GLFW
		glfwShowWindow(window);
	}

	public void update() {
		glfwPollEvents();
		if (inputController != null) {
			inputController.update();
		}
	}

	public void render() {
		glfwSwapBuffers(window);
	}

	public boolean shouldClose() {
		return glfwWindowShouldClose(window);
	}

	public void destroy() {
		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);
		glfwTerminate();
	}
}
