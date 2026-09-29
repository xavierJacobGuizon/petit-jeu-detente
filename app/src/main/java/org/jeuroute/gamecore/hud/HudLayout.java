package org.jeuroute.gamecore.hud;

import java.util.ArrayList;
import java.util.List;

public final class HudLayout {

	static final int SLOT_ITEM_WIDTH = 128;
	static final int SLOT_HEIGHT = 62;
	static final int BUTTON_WIDTH = 152;
	static final int BUTTON_HEIGHT = 38;
	static final int GAP = 8;
	static final int MARGIN = 16;
	static final int DIALOG_WIDTH = 440;
	static final int DIALOG_HEADER_HEIGHT = 54;
	static final int DIALOG_ROW_HEIGHT = 42;
	static final int DIALOG_PADDING = 12;

	public record SlotPlacement(HudIndicatorSlot slot, Bounds bounds) {}

	public record DialogPlacement(
		Bounds panel,
		List<Bounds> options,
		Bounds previous,
		Bounds next
	) {}

	public record Bounds(int x, int y, int width, int height) {
		public boolean contains(double pointX, double pointY) {
			return pointX >= x && pointX <= x + width && pointY >= y && pointY <= y + height;
		}

		public boolean intersects(Bounds other) {
			return (
				x < other.x + other.width &&
				x + width > other.x &&
				y < other.y + other.height &&
				y + height > other.y
			);
		}
	}

	private HudLayout() {}

	static int dialogPageSize(int optionCount, int windowHeight) {
		int maxRows = Math.max(
			1,
			(windowHeight - MARGIN * 2 - DIALOG_HEADER_HEIGHT - DIALOG_PADDING * 2) /
				DIALOG_ROW_HEIGHT
		);
		return optionCount > maxRows ? Math.max(1, maxRows - 1) : maxRows;
	}

	static DialogPlacement dialogPlacement(
		int optionCount,
		int page,
		int windowWidth,
		int windowHeight
	) {
		int pageSize = dialogPageSize(optionCount, windowHeight);
		int firstOption = page * pageSize;
		int visibleRows = Math.min(pageSize, Math.max(0, optionCount - firstOption));
		boolean paginated = optionCount > pageSize;
		int height =
			DIALOG_HEADER_HEIGHT +
			DIALOG_PADDING * 2 +
			(visibleRows + (paginated ? 1 : 0)) * DIALOG_ROW_HEIGHT;
		int x = (windowWidth - DIALOG_WIDTH) / 2;
		int y = (windowHeight - height) / 2;
		Bounds panel = new Bounds(x, y, DIALOG_WIDTH, height);
		List<Bounds> options = new ArrayList<>();
		for (int index = 0; index < visibleRows; index++) {
			int optionX = x + DIALOG_PADDING;
			int optionY = y + DIALOG_HEADER_HEIGHT + DIALOG_PADDING + index * DIALOG_ROW_HEIGHT;
			options.add(
				new Bounds(
					optionX,
					optionY,
					DIALOG_WIDTH - DIALOG_PADDING * 2,
					DIALOG_ROW_HEIGHT - 4
				)
			);
		}
		Bounds previous = null;
		Bounds next = null;
		if (paginated) {
			int navY = y + DIALOG_HEADER_HEIGHT + DIALOG_PADDING + visibleRows * DIALOG_ROW_HEIGHT;
			int navWidth = (DIALOG_WIDTH - DIALOG_PADDING * 2 - GAP) / 2;
			previous = new Bounds(x + DIALOG_PADDING, navY, navWidth, DIALOG_ROW_HEIGHT - 4);
			next = new Bounds(
				x + DIALOG_PADDING + navWidth + GAP,
				navY,
				navWidth,
				DIALOG_ROW_HEIGHT - 4
			);
		}
		return new DialogPlacement(panel, List.copyOf(options), previous, next);
	}

	public static List<SlotPlacement> layoutIndicatorSlots(
		List<HudIndicatorSlot> slots,
		int windowWidth,
		int windowHeight
	) {
		List<SlotPlacement> placements = new ArrayList<>();
		int usableWidth = windowWidth - MARGIN * 2;
		int usableHeight = windowHeight - MARGIN * 2;

		for (HudIndicatorSlot slot : slots) {
			int cellWidth = SLOT_ITEM_WIDTH;
			int width = Math.max(1, slot.getIndicators().size()) * cellWidth;
			int height = SLOT_HEIGHT;
			if (width > usableWidth || height > usableHeight) {
				throw new IllegalArgumentException(
					"HUD indicator slot does not fit inside the window"
				);
			}

			Bounds placement = findPlacement(
				slot.getAnchor(),
				width,
				height,
				usableWidth,
				usableHeight,
				placements
			);
			if (placement == null) {
				throw new IllegalStateException(
					"No non-overlapping position is available for HUD indicator slot"
				);
			}
			placements.add(new SlotPlacement(slot, placement));
		}
		return List.copyOf(placements);
	}

	private static Bounds findPlacement(
		HudAnchor anchor,
		int width,
		int height,
		int usableWidth,
		int usableHeight,
		List<SlotPlacement> occupied
	) {
		int horizontalStep = width + GAP;
		int verticalStep = height + GAP;
		for (
			int verticalOffset = 0;
			verticalOffset <= usableHeight - height;
			verticalOffset += verticalStep
		) {
			for (
				int horizontalOffset = 0;
				horizontalOffset <= usableWidth - width;
				horizontalOffset += horizontalStep
			) {
				int x = isRight(anchor)
					? MARGIN + usableWidth - width - horizontalOffset
					: MARGIN + horizontalOffset;
				int y = isBottom(anchor)
					? MARGIN + usableHeight - height - verticalOffset
					: MARGIN + verticalOffset;
				Bounds candidate = new Bounds(x, y, width, height);
				if (
					occupied.stream().noneMatch(existing -> candidate.intersects(existing.bounds()))
				) {
					return candidate;
				}
			}
		}
		return null;
	}

	private static boolean isRight(HudAnchor anchor) {
		return anchor == HudAnchor.TOP_RIGHT || anchor == HudAnchor.BOTTOM_RIGHT;
	}

	private static boolean isBottom(HudAnchor anchor) {
		return anchor == HudAnchor.BOTTOM_LEFT || anchor == HudAnchor.BOTTOM_RIGHT;
	}

	static Bounds rootButtonBounds(int index, int windowWidth, int windowHeight) {
		int x = windowWidth - MARGIN - BUTTON_WIDTH - index * (BUTTON_WIDTH + GAP);
		int y = windowHeight - MARGIN - BUTTON_HEIGHT;
		return new Bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
	}

	static Bounds submenuButtonBounds(
		int rootIndex,
		int submenuIndex,
		int windowWidth,
		int windowHeight
	) {
		Bounds root = rootButtonBounds(rootIndex, windowWidth, windowHeight);
		int y = root.y() - GAP - BUTTON_HEIGHT - submenuIndex * (BUTTON_HEIGHT + GAP);
		return new Bounds(root.x(), y, BUTTON_WIDTH, BUTTON_HEIGHT);
	}
}
