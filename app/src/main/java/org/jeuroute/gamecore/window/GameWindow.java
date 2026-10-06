package org.jeuroute.gamecore.window;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.IntBuffer;
import java.util.Objects;
import org.jeuroute.gamecore.camera.Camera2D;
import org.jeuroute.gamecore.controllers.WindowInputController;
import org.jeuroute.model.records.window.WindowMetrics;
import org.lwjgl.system.MemoryStack;

public class GameWindow {

	private int width;
	private int height;
	private int framebufferWidth;
	private int framebufferHeight;

	private final String title;

	// C'est la fenêtre GLFW (Graphics Library Framework), un accronyme qui veux dire "Cadre de travail pour les graphiques".
	// Elle est représentée par un identifiant de type long qui sert à référencer la fenêtre dans les appels GLFW.
	private long window;
	private final Camera2D camera = new Camera2D(640.0, 360.0);

	private WindowInputController inputController;

	public GameWindow(int width, int height, String title) {
		this.width = width;
		this.height = height;
		framebufferWidth = width;
		framebufferHeight = height;
		this.title = title;
	}

	public Camera2D getCamera() {
		return camera;
	}

	public WindowMetrics getMetrics() {
		return new WindowMetrics(width, height, framebufferWidth, framebufferHeight);
	}

	public void setInputController(WindowInputController inputController) {
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

	public void requestClose() {
		glfwSetWindowShouldClose(window, true);
	}
}
