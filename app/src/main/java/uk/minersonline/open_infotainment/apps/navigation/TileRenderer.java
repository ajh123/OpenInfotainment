package uk.minersonline.open_infotainment.apps.navigation;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class TileRenderer {
	private long window;
	private int viewportWidth = 800;
	private int viewportHeight = 600;
	private final int TILE_SIZE = 256;

	private final TileDownloader downloader = new TileDownloader();
	private final Map<String, ByteBuffer> tileDataCache = new ConcurrentHashMap<>();
	private final Map<String, Integer> textureCache = new ConcurrentHashMap<>();
	private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
	private final ExecutorService executor = Executors.newFixedThreadPool(4);

	public void run() {
		init();
		loop();
		cleanup();
	}

	private void init() {
		GLFWErrorCallback.createPrint(System.err).set();
		if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");

		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

		window = glfwCreateWindow(viewportWidth, viewportHeight, "Tile Renderer", NULL, NULL);
		if (window == NULL) throw new RuntimeException("Failed to create the GLFW window");

		glfwSetFramebufferSizeCallback(window, (win, width, height) -> {
			viewportWidth = width;
			viewportHeight = height;
			glViewport(0, 0, width, height);
			glMatrixMode(GL_PROJECTION);
			glLoadIdentity();
			glOrtho(0, width, height, 0, -1, 1);
			glMatrixMode(GL_MODELVIEW);
		});

		glfwMakeContextCurrent(window);
		glfwSwapInterval(1);
		glfwShowWindow(window);
	}

	private void loop() {
		GL.createCapabilities();

		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		glEnable(GL_TEXTURE_2D);

		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glOrtho(0, viewportWidth, viewportHeight, 0, -1, 1);
		glMatrixMode(GL_MODELVIEW);

		while (!glfwWindowShouldClose(window)) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			glLoadIdentity();

			// Determine the visible tiles based on the viewport size
			int startX = 0;
			int startY = 0;
			int endX = (viewportWidth / TILE_SIZE) + 1;
			int endY = (viewportHeight / TILE_SIZE) + 1;

			// Render tiles within the visible range
			renderWorldTiles(startX, startY, endX, endY);

			// Process texture uploads and other tasks
			processTasks();

			glfwSwapBuffers(window);
			glfwPollEvents();
		}
	}

	private void renderWorldTiles(int startX, int startY, int endX, int endY) {
		for (int x = startX; x < endX; x++) {
			for (int y = startY; y < endY; y++) {
				final int tileX = x;
				final int tileY = y;
				final int zoomLevel = 1; // Example zoom level

				final String key = tileX + "_" + tileY + "_" + zoomLevel;
				if (!textureCache.containsKey(key)) {
					// Queue tile downloading for the background thread
					executor.submit(() -> downloadTileData(tileX, tileY, zoomLevel, key));
				}

				// Render the tile if it is available
				int textureId = textureCache.getOrDefault(key, -1);
				if (textureId != -1) {
					renderTilePlane(textureId, tileX * TILE_SIZE, tileY * TILE_SIZE, TILE_SIZE, TILE_SIZE);
				}
			}
		}
	}

	private void downloadTileData(int x, int y, int z, String key) {
		try {
			byte[] imageData = downloader.getTile(x, y, z);

			if (imageData == null || imageData.length == 0) {
				return; // Skip if no data
			}

			ByteBuffer imageBuffer = ByteBuffer.wrap(imageData);
			imageBuffer.flip(); // Prepare the buffer for reading
			tileDataCache.put(key, imageBuffer);

			// Queue texture upload for the main thread
			tasks.add(() -> uploadTileTexture(key));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private void uploadTileTexture(String key) {
		// Ensure this runs on the main thread
		if (glfwGetCurrentContext() != window) {
			throw new IllegalStateException("OpenGL context is not current on this thread");
		}

		ByteBuffer imageBuffer = tileDataCache.remove(key);

		// Check if buffer is valid
		if (imageBuffer == null) {
			System.err.println("Invalid image buffer.");
			return;
		}

		try (MemoryStack stack = stackPush()) {
			IntBuffer width = stack.mallocInt(1);
			IntBuffer height = stack.mallocInt(1);
			IntBuffer channels = stack.mallocInt(1);

			// Flip the buffer before reading
			imageBuffer.flip();

			ByteBuffer buffer = STBImage.stbi_load_from_memory(imageBuffer, width, height, channels, 4);
			if (buffer == null) {
				System.err.println("Failed to load image data: " + STBImage.stbi_failure_reason());
				return; // Failed to load image
			}

			int textureId = glGenTextures();
			glBindTexture(GL_TEXTURE_2D, textureId);

			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(), height.get(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

			STBImage.stbi_image_free(buffer);

			textureCache.put(key, textureId);
		}
	}


	private void renderTilePlane(int textureId, float x, float y, float width, float height) {
		glBindTexture(GL_TEXTURE_2D, textureId);

		glBegin(GL_QUADS);
		glTexCoord2f(0, 0); glVertex3f(x, y, 0);
		glTexCoord2f(1, 0); glVertex3f(x + width, y, 0);
		glTexCoord2f(1, 1); glVertex3f(x + width, y + height, 0);
		glTexCoord2f(0, 1); glVertex3f(x, y + height, 0);
		glEnd();
	}

	private void processTasks() {
		while (!tasks.isEmpty()) {
			Runnable task = tasks.poll();
			if (task != null) {
				// Ensure the task runs on the main thread
				task.run();
			}
		}
	}

	private void cleanup() {
		// Delete textures
		for (int textureId : textureCache.values()) {
			glDeleteTextures(textureId);
		}

		executor.shutdown();
		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

	public static void main(String[] args) {
		new TileRenderer().run();
	}
}
