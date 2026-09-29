package org.jeuroute.gamecore.hud;

import java.util.List;
import java.util.Objects;

public final class HudDialog {

	private final String title;
	private final List<HudButton> options;
	private int page;

	public HudDialog(String title, List<HudButton> options) {
		this.title = Objects.requireNonNull(title);
		this.options = List.copyOf(options);
	}

	public String getTitle() {
		return title;
	}

	public List<HudButton> getOptions() {
		return options;
	}

	public List<HudButton> getVisibleOptions(int windowHeight) {
		int pageSize = HudLayout.dialogPageSize(options.size(), windowHeight);
		int firstOption = page * pageSize;
		return options.subList(firstOption, Math.min(firstOption + pageSize, options.size()));
	}

	public int getPage() {
		return page;
	}

	public int getPageCount(int windowHeight) {
		int pageSize = HudLayout.dialogPageSize(options.size(), windowHeight);
		return Math.max(1, (options.size() + pageSize - 1) / pageSize);
	}

	public boolean hasPreviousPage() {
		return page > 0;
	}

	public boolean hasNextPage(int windowHeight) {
		return page + 1 < getPageCount(windowHeight);
	}

	public void previousPage() {
		page = Math.max(0, page - 1);
	}

	public void nextPage(int windowHeight) {
		page = Math.min(getPageCount(windowHeight) - 1, page + 1);
	}
}
