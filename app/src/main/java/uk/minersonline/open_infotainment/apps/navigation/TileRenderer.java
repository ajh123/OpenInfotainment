package uk.minersonline.open_infotainment.apps.navigation;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class TileRenderer {
	private long window;

	// Tile properties
	private static final int TILE_SIZE = 256; // Assuming tiles are 256x256 pixels

	public void run() {
		init();
		loop();

		// Free resources and terminate
		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

	private void init() {
		GLFWErrorCallback.createPrint(System.err).set();
		if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");

		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

		window = glfwCreateWindow(800, 600, "Tile Renderer", NULL, NULL);
		if (window == NULL) throw new RuntimeException("Failed to create the GLFW window");

		glfwMakeContextCurrent(window);
		glfwSwapInterval(1);
		glfwShowWindow(window);
	}

	private void loop() {
		GL.createCapabilities();

		// Enable transparency
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		// Enable textures
		glEnable(GL_TEXTURE_2D);

		// Simple shader setup
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glOrtho(0, 800, 600, 0, -1, 1);
		glMatrixMode(GL_MODELVIEW);

		// Load and bind the texture for a tile
		int textureId = loadTileTexture(0, 0, 1); // Example coordinates

		while (!glfwWindowShouldClose(window)) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			glLoadIdentity();

			// Render the tile on a plane
			renderTilePlane(textureId, 100, 100, TILE_SIZE, TILE_SIZE);

			glfwSwapBuffers(window);
			glfwPollEvents();
		}

		// Clean up texture
		glDeleteTextures(textureId);
	}

	private int loadTileTexture(int x, int y, int z) {
		TileDownloader downloader = new TileDownloader();
		try {
			byte[] imageData = downloader.getTile(x, y, z);

			// Check if image data is valid
			if (imageData == null || imageData.length == 0) {
				throw new RuntimeException("Image data is null or empty");
			}

			// Use a memory stack to allocate native buffers
			try (MemoryStack stack = MemoryStack.stackPush()) {
				IntBuffer width = stack.mallocInt(1);
				IntBuffer height = stack.mallocInt(1);
				IntBuffer channels = stack.mallocInt(1);

				// Create a ByteBuffer with native order
				ByteBuffer imageBuffer = stack.malloc(imageData.length).put(imageData);
				imageBuffer.flip(); // Prepare the buffer for reading

				// Load the image using STBImage
				ByteBuffer buffer = STBImage.stbi_load_from_memory(imageBuffer, width, height, channels, 4);
				if (buffer == null) {
					throw new RuntimeException("Failed to load image: " + STBImage.stbi_failure_reason());
				}

				// Generate and bind a new OpenGL texture
				int textureId = glGenTextures();
				glBindTexture(GL_TEXTURE_2D, textureId);

				// Set texture parameters
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

				// Upload the texture data to OpenGL
				glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(), height.get(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

				// Free the image memory
				STBImage.stbi_image_free(buffer);

				return textureId;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return -1; // Error case if texture loading fails
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

	public static void main(String[] args) {
		new TileRenderer().run();
	}
}
