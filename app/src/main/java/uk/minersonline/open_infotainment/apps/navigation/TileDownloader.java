package uk.minersonline.open_infotainment.apps.navigation;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

public class TileDownloader {
	private static final String TILE_URL = "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";
	private final OkHttpClient client = new OkHttpClient();

	public byte[] downloadTile(int x, int y, int z) throws IOException {
		String url = TILE_URL.replace("{s}", "a") // Use "a", "b", or "c" for subdomains
				.replace("{z}", String.valueOf(z))
				.replace("{x}", String.valueOf(x))
				.replace("{y}", String.valueOf(y));

		Request request = new Request.Builder()
				.url(url)
				.header("User-Agent", "OpenInfotainment/0.1.0 (https://github.com/ajh123/OpenInfotainment/)")
				.build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Failed to download tile: " + response);
			}

			return response.body().bytes();
		}
	}

	public void saveTileToCache(int x, int y, int z, byte[] tileData) throws IOException {
		File cacheDir = new File("tile_cache/" + z + "/" + x);
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}

		File tileFile = new File(cacheDir, y + ".png");
		try (FileOutputStream fos = new FileOutputStream(tileFile)) {
			fos.write(tileData);
		}
	}

	public byte[] getTile(int x, int y, int z) throws IOException {
		File tileFile = new File("tile_cache/" + z + "/" + x + "/" + y + ".png");
		if (tileFile.exists()) {
			return Files.readAllBytes(tileFile.toPath());
		} else {
			byte[] tileData = downloadTile(x, y, z);
			saveTileToCache(x, y, z, tileData);
			return tileData;
		}
	}
}
