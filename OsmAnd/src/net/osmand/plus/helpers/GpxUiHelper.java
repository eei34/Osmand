package net.osmand.plus.helpers;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.SwitchCompat;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.MPPointF;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.Elevation;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.GPXUtilities.Speed;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.ActivityResultListener;
import net.osmand.plus.activities.ActivityResultListener.OnActivityResultListener;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.dialogs.ConfigureMapMenu.AppearanceListItem;
import net.osmand.plus.dialogs.ConfigureMapMenu.GpxAppearanceAdapter;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM;
import static net.osmand.plus.OsmAndFormatter.FEET_IN_ONE_METER;
import static net.osmand.plus.OsmAndFormatter.METERS_IN_KILOMETER;
import static net.osmand.plus.OsmAndFormatter.METERS_IN_ONE_MILE;
import static net.osmand.plus.OsmAndFormatter.METERS_IN_ONE_NAUTICALMILE;
import static net.osmand.plus.OsmAndFormatter.YARDS_IN_ONE_METER;
import static net.osmand.plus.dialogs.ConfigureMapMenu.CURRENT_TRACK_COLOR_ATTR;
import static net.osmand.plus.dialogs.ConfigureMapMenu.CURRENT_TRACK_WIDTH_ATTR;
import static net.osmand.plus.download.DownloadActivity.formatMb;

public class GpxUiHelper {

	private static final int OPEN_GPX_DOCUMENT_REQUEST = 1005;

	public static String getDescription(OsmandApplication app, GPXFile result, File f, boolean html) {
		GPXTrackAnalysis analysis = result.getAnalysis(f == null ? 0 : f.lastModified());
		return getDescription(app, analysis, html);
	}

	public static String getDescription(OsmandApplication app, TrkSegment t, boolean html) {
		return getDescription(app, GPXTrackAnalysis.segment(0, t), html);
	}


	public static String getColorValue(String clr, String value, boolean html) {
		if (!html) {
			return value;
		}
		return "<font color=\"" + clr + "\">" + value + "</font>";
	}

	public static String getColorValue(String clr, String value) {
		return getColorValue(clr, value, true);
	}

	public static String getDescription(OsmandApplication app, GPXTrackAnalysis analysis, boolean html) {
		StringBuilder description = new StringBuilder();
		String nl = html ? "<br/>" : "\n";
		String timeSpanClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_time_span_color));
		String distanceClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_distance_color));
		String speedClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_speed));
		String ascClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_altitude_asc));
		String descClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_altitude_desc));
		// OUTPUT:
		// 1. Total distance, Start time, End time
		description.append(app.getString(R.string.gpx_info_distance, getColorValue(distanceClr,
						OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app), html),
				getColorValue(distanceClr, analysis.points + "", html)));
		if (analysis.totalTracks > 1) {
			description.append(nl).append(app.getString(R.string.gpx_info_subtracks, getColorValue(speedClr, analysis.totalTracks + "", html)));
		}
		if (analysis.wptPoints > 0) {
			description.append(nl).append(app.getString(R.string.gpx_info_waypoints, getColorValue(speedClr, analysis.wptPoints + "", html)));
		}
		if (analysis.isTimeSpecified()) {
			description.append(nl).append(app.getString(R.string.gpx_info_start_time, analysis.startTime));
			description.append(nl).append(app.getString(R.string.gpx_info_end_time, analysis.endTime));
		}

		// 2. Time span
		if (analysis.timeSpan > 0 && analysis.timeSpan / 1000 != analysis.timeMoving / 1000) {
			final String formatDuration = Algorithms.formatDuration((int) (analysis.timeSpan / 1000), app.accessibilityEnabled());
			description.append(nl).append(app.getString(R.string.gpx_timespan,
					getColorValue(timeSpanClr, formatDuration, html)));
		}

		// 3. Time moving, if any
		if (analysis.isTimeMoving()) {
				//Next few lines for Issue 3222 heuristic testing only
				//final String formatDuration0 = Algorithms.formatDuration((int) (analysis.timeMoving0 / 1000), app.accessibilityEnabled());
				//description.append(nl).append(app.getString(R.string.gpx_timemoving,
				//		getColorValue(timeSpanClr, formatDuration0, html)));
				//description.append(" (" + getColorValue(distanceClr, OsmAndFormatter.getFormattedDistance(analysis.totalDistanceMoving0, app), html) + ")");
			final String formatDuration = Algorithms.formatDuration((int) (analysis.timeMoving / 1000), app.accessibilityEnabled());
			description.append(nl).append(app.getString(R.string.gpx_timemoving,
					getColorValue(timeSpanClr, formatDuration, html)));
			description.append(" (" + getColorValue(distanceClr, OsmAndFormatter.getFormattedDistance(analysis.totalDistanceMoving, app), html) + ")");
		}

		// 4. Elevation, eleUp, eleDown, if recorded
		if (analysis.isElevationSpecified()) {
			description.append(nl);
			description.append(app.getString(R.string.gpx_info_avg_altitude,
					getColorValue(speedClr, OsmAndFormatter.getFormattedAlt(analysis.avgElevation, app), html)));
			description.append(nl);
			String min = getColorValue(descClr, OsmAndFormatter.getFormattedAlt(analysis.minElevation, app), html);
			String max = getColorValue(ascClr, OsmAndFormatter.getFormattedAlt(analysis.maxElevation, app), html);
			String asc = getColorValue(ascClr, OsmAndFormatter.getFormattedAlt(analysis.diffElevationUp, app), html);
			String desc = getColorValue(descClr, OsmAndFormatter.getFormattedAlt(analysis.diffElevationDown, app), html);
			description.append(app.getString(R.string.gpx_info_diff_altitude, min + " - " + max));
			description.append(nl);
			description.append(app.getString(R.string.gpx_info_asc_altitude, "\u2193 " + desc + "   \u2191 " + asc + ""));
		}


		if (analysis.isSpeedSpecified()) {
			String avg = getColorValue(speedClr, OsmAndFormatter.getFormattedSpeed(analysis.avgSpeed, app), html);
			String max = getColorValue(ascClr, OsmAndFormatter.getFormattedSpeed(analysis.maxSpeed, app), html);
			description.append(nl).append(app.getString(R.string.gpx_info_average_speed, avg));
			description.append(nl).append(app.getString(R.string.gpx_info_maximum_speed, max));
		}
		return description.toString();
	}

	public static AlertDialog selectGPXFiles(List<String> selectedGpxList, final Activity activity,
											 final CallbackWithObject<GPXFile[]> callbackWithObject) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		final File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		final List<GPXInfo> allGpxList = getSortedGPXFilesInfo(dir, selectedGpxList, false);
		if (allGpxList.isEmpty()) {
			Toast.makeText(activity, R.string.gpx_files_not_found, Toast.LENGTH_LONG).show();
		}
		allGpxList.add(0, new GPXInfo(activity.getString(R.string.show_current_gpx_title), 0, 0));
		final ContextMenuAdapter adapter = createGpxContextMenuAdapter(allGpxList, selectedGpxList, true);

		return createDialog(activity, true, true, true, callbackWithObject, allGpxList, adapter);
	}

	public static AlertDialog selectGPXFile(final Activity activity,
											final boolean showCurrentGpx, final boolean multipleChoice, final CallbackWithObject<GPXFile[]> callbackWithObject) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		final File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		final List<GPXInfo> list = getSortedGPXFilesInfo(dir, null, false);
		if (list.isEmpty()) {
			Toast.makeText(activity, R.string.gpx_files_not_found, Toast.LENGTH_LONG).show();
		}
		if (!list.isEmpty() || showCurrentGpx) {
			if (showCurrentGpx) {
				list.add(0, new GPXInfo(activity.getString(R.string.show_current_gpx_title), 0, 0));
			}

			final ContextMenuAdapter adapter = createGpxContextMenuAdapter(list, null, showCurrentGpx);
			return createDialog(activity, showCurrentGpx, multipleChoice, false, callbackWithObject, list, adapter);
		}
		return null;
	}

	public static AlertDialog selectSingleGPXFile(final Activity activity, boolean showCurrentGpx,
												  final CallbackWithObject<GPXFile[]> callbackWithObject) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		int gpxDirLength = app.getAppPath(IndexConstants.GPX_INDEX_DIR).getAbsolutePath().length();
		List<SelectedGpxFile> selectedGpxFiles = app.getSelectedGpxHelper().getSelectedGPXFiles();
		final List<GPXInfo> list = new ArrayList<>(selectedGpxFiles.size() + 1);
		if (OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class) == null) {
			showCurrentGpx = false;
		}
		if (!selectedGpxFiles.isEmpty() || showCurrentGpx) {
			if (showCurrentGpx) {
				list.add(new GPXInfo(activity.getString(R.string.shared_string_currently_recording_track), 0, 0));
			}

			for (SelectedGpxFile selectedGpx : selectedGpxFiles) {
				if (!selectedGpx.getGpxFile().showCurrentTrack) {
					list.add(new GPXInfo(selectedGpx.getGpxFile().path.substring(gpxDirLength + 1), selectedGpx.getGpxFile().modifiedTime, 0));
				}
			}

			final ContextMenuAdapter adapter = createGpxContextMenuAdapter(list, null, showCurrentGpx);
			return createSingleChoiceDialog(activity, showCurrentGpx, callbackWithObject, list, adapter);
		}
		return null;
	}

	private static ContextMenuAdapter createGpxContextMenuAdapter(List<GPXInfo> allGpxList,
																  List<String> selectedGpxList,
																  boolean showCurrentTrack) {
		final ContextMenuAdapter adapter = new ContextMenuAdapter();
		//element position in adapter
		int i = 0;
		for (GPXInfo gpxInfo : allGpxList) {
			String s = gpxInfo.getFileName();
			String fileName = s;
			if (s.endsWith(".gpx")) {
				s = s.substring(0, s.length() - ".gpx".length());
			}
			s = s.replace('_', ' ');

			adapter.addItem(ContextMenuItem.createBuilder(s).setSelected(false)
					.setIcon(R.drawable.ic_action_polygom_dark).createItem());

			//if there's some selected files - need to mark them as selected
			if (selectedGpxList != null) {
				updateSelection(selectedGpxList, showCurrentTrack, adapter, i, fileName);
			}
			i++;
		}
		return adapter;
	}

	protected static void updateSelection(List<String> selectedGpxList, boolean showCurrentTrack,
										  final ContextMenuAdapter adapter, int position, String fileName) {
		ContextMenuItem item = adapter.getItem(position);
		if (position == 0 && showCurrentTrack) {
			if (selectedGpxList.contains("")) {
				item.setSelected(true);
			}
		} else {
			for (String file : selectedGpxList) {
				if (file.endsWith(fileName)) {
					item.setSelected(true);
					break;
				}
			}
		}
	}

	private static void setDescripionInDialog(final ArrayAdapter<?> adapter, final ContextMenuAdapter cmAdapter, Activity activity,
											  final File dir, String filename, final int position) {
		final Application app = activity.getApplication();
		final File f = new File(dir, filename);
		loadGPXFileInDifferentThread(activity, new CallbackWithObject<GPXUtilities.GPXFile[]>() {

			@Override
			public boolean processResult(GPXFile[] result) {
				ContextMenuItem item = cmAdapter.getItem(position);
				item.setTitle(item.getTitle() + "\n" + getDescription((OsmandApplication) app, result[0], f, false));
				adapter.notifyDataSetInvalidated();
				return true;
			}
		}, dir, null, filename);
	}

	private static AlertDialog createSingleChoiceDialog(final Activity activity,
											final boolean showCurrentGpx,
											final CallbackWithObject<GPXFile[]> callbackWithObject,
											final List<GPXInfo> list,
											final ContextMenuAdapter adapter) {
		final OsmandApplication app = (OsmandApplication) activity.getApplication();
		final IconsCache iconsCache = app.getIconsCache();
		final File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		final int layout = R.layout.list_menu_item_native_singlechoice;

		final ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(activity, layout, R.id.text1,
				adapter.getItemNames()) {
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				if (v == null) {
					v = activity.getLayoutInflater().inflate(layout, null);
				}
				final ContextMenuItem item = adapter.getItem(position);
				TextView tv = (TextView) v.findViewById(R.id.text1);
				Drawable icon;
				if (showCurrentGpx && position == 0) {
					icon = null;
				} else {
					icon = iconsCache.getThemedIcon(item.getIcon());
				}
				tv.setCompoundDrawablePadding(AndroidUtils.dpToPx(activity, 10f));
				tv.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
				tv.setText(item.getTitle());
				tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

				return v;
			}
		};

		int selectedIndex = 0;
		String prevSelectedGpx = app.getSettings().LAST_SELECTED_GPX_TRACK_FOR_NEW_POINT.get();
		if (prevSelectedGpx != null) {
			selectedIndex = list.indexOf(prevSelectedGpx);
		}
		if (selectedIndex == -1) {
			selectedIndex = 0;
		}

		final int[] selectedPosition = {selectedIndex};
		builder.setSingleChoiceItems(listAdapter, selectedIndex, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int position) {
				selectedPosition[0] = position;
			}
		});
		builder.setTitle(R.string.select_gpx)
				.setPositiveButton(R.string.shared_string_ok, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						int position = selectedPosition[0];
						if (position != -1) {
							if (showCurrentGpx && position == 0) {
								callbackWithObject.processResult(null);
								app.getSettings().LAST_SELECTED_GPX_TRACK_FOR_NEW_POINT.set(null);
							} else {
								String fileName = list.get(position).getFileName();
								app.getSettings().LAST_SELECTED_GPX_TRACK_FOR_NEW_POINT.set(fileName);
								SelectedGpxFile selectedGpxFile =
										app.getSelectedGpxHelper().getSelectedFileByName(fileName);
								if (selectedGpxFile != null) {
									callbackWithObject.processResult(new GPXFile[]{selectedGpxFile.getGpxFile()});
								} else {
									loadGPXFileInDifferentThread(activity, callbackWithObject, dir, null, fileName);
								}
							}
						}
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null);

		final AlertDialog dlg = builder.create();
		dlg.setCanceledOnTouchOutside(false);
		dlg.show();
		try {
			dlg.getListView().setFastScrollEnabled(true);
		} catch (Exception e) {
			// java.lang.ClassCastException: com.android.internal.widget.RoundCornerListAdapter
			// Unknown reason but on some devices fail
		}
		return dlg;
	}

	private static AlertDialog createDialog(final Activity activity,
											final boolean showCurrentGpx,
											final boolean multipleChoice,
											final boolean showAppearanceSetting,
											final CallbackWithObject<GPXFile[]> callbackWithObject,
											final List<GPXInfo> list,
											final ContextMenuAdapter adapter) {
		final OsmandApplication app = (OsmandApplication) activity.getApplication();
		final DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(activity);
		final File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		final int layout = R.layout.list_item_with_checkbox;
		final int switchLayout = R.layout.list_item_with_switch;
		final Map<String, String> gpxAppearanceParams = new HashMap<>();

		final ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(activity, layout, R.id.title,
				adapter.getItemNames()) {

			@Override
			public int getItemViewType(int position) {
				return showCurrentGpx && position == 0 ? 1 : 0;
			}

			@Override
			public int getViewTypeCount() {
				return 2;
			}

			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				if (v == null) {
					if (getItemViewType(position) == 0) {
						v = activity.getLayoutInflater().inflate(layout, null);
					} else {
						v = activity.getLayoutInflater().inflate(switchLayout, null);
					}
				}

				TextView tv = (TextView) v.findViewById(R.id.title);
				TextView dv = (TextView) v.findViewById(R.id.description);
				final ContextMenuItem item = adapter.getItem(position);

				if (showCurrentGpx && position == 0) {
					tv.setText(item.getTitle());
					dv.setText(OsmAndFormatter.getFormattedDistance(app.getSavingTrackHelper().getDistance(), app));
					final SwitchCompat ch = ((SwitchCompat) v.findViewById(R.id.toggle_item));
					ch.setOnCheckedChangeListener(null);
					ch.setChecked(item.getSelected());
					ch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							item.setSelected(isChecked);
						}
					});
					return v;
				}

				tv.setText(item.getTitle().replace("/", " • "));
				GPXInfo info = list.get(position);
				StringBuilder sb = new StringBuilder();
				if (info.getLastModified() > 0) {
					sb.append(dateFormat.format(info.getLastModified()));
				}
				if (info.getFileSize() >= 0) {
					if (sb.length() > 0) {
						sb.append(" • ");
					}
					long fileSizeKB = info.getFileSize() / 1000;
					if (info.getFileSize() < 5000) {
						sb.append(info.getFileSize()).append(" B");
					} else if (fileSizeKB > 100) {
						sb.append(formatMb.format(new Object[]{(float) fileSizeKB / (1 << 10)}));
					} else {
						sb.append(fileSizeKB).append(" kB");
					}
				}
				dv.setText(sb.toString());

				/*
				final ArrayAdapter<String> arrayAdapter = this;
				iconView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						int nline = item.getTitle().indexOf('\n');
						if (nline == -1) {
							String fileName = list.get(position).getFileName();
							setDescripionInDialog(arrayAdapter, adapter, activity, dir, fileName, position);
						} else {
							item.setTitle(item.getTitle().substring(0, nline));
							arrayAdapter.notifyDataSetInvalidated();
						}
					}

				});
				*/


				final CheckBox ch = ((CheckBox) v.findViewById(R.id.toggle_item));
				if (item.getSelected() == null) {
					ch.setVisibility(View.INVISIBLE);
				} else {
					ch.setOnCheckedChangeListener(null);
					ch.setChecked(item.getSelected());
					ch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							item.setSelected(isChecked);
						}
					});
				}
				return v;
			}
		};

		OnClickListener onClickListener = new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int position) {
			}
		};
		builder.setAdapter(listAdapter, onClickListener);
		if (multipleChoice) {
			if (showAppearanceSetting) {
				final RenderingRuleProperty trackWidthProp;
				final RenderingRuleProperty trackColorProp;
				final RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
				if (renderer != null) {
					trackWidthProp = renderer.PROPS.getCustomRule(CURRENT_TRACK_WIDTH_ATTR);
					trackColorProp = renderer.PROPS.getCustomRule(CURRENT_TRACK_COLOR_ATTR);
				} else {
					trackWidthProp = null;
					trackColorProp = null;
				}
				if (trackWidthProp == null || trackColorProp == null) {
					builder.setTitle(R.string.show_gpx);
				} else {
					final View apprTitleView = activity.getLayoutInflater().inflate(R.layout.select_gpx_appearance_title, null);

					final OsmandSettings.CommonPreference<String> prefWidth
							= app.getSettings().getCustomRenderProperty(CURRENT_TRACK_WIDTH_ATTR);
					final OsmandSettings.CommonPreference<String> prefColor
							= app.getSettings().getCustomRenderProperty(CURRENT_TRACK_COLOR_ATTR);

					updateAppearanceTitle(activity, app, trackWidthProp, renderer, apprTitleView, prefWidth.get(), prefColor.get());

					apprTitleView.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							final ListPopupWindow popup = new ListPopupWindow(activity);
							popup.setAnchorView(apprTitleView);
							popup.setContentWidth(AndroidUtils.dpToPx(activity, 200f));
							popup.setModal(true);
							popup.setDropDownGravity(Gravity.RIGHT | Gravity.TOP);
							popup.setVerticalOffset(AndroidUtils.dpToPx(activity, -48f));
							popup.setHorizontalOffset(AndroidUtils.dpToPx(activity, -6f));
							final GpxAppearanceAdapter gpxApprAdapter = new GpxAppearanceAdapter(activity,
									gpxAppearanceParams.containsKey(CURRENT_TRACK_COLOR_ATTR) ? gpxAppearanceParams.get(CURRENT_TRACK_COLOR_ATTR) : prefColor.get(),
									GpxAppearanceAdapter.GpxAppearanceAdapterType.TRACK_WIDTH_COLOR);
							popup.setAdapter(gpxApprAdapter);
							popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {

								@Override
								public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
									AppearanceListItem item = gpxApprAdapter.getItem(position);
									if (item != null) {
										if (item.getAttrName() == CURRENT_TRACK_WIDTH_ATTR) {
											gpxAppearanceParams.put(CURRENT_TRACK_WIDTH_ATTR, item.getValue());
										} else if (item.getAttrName() == CURRENT_TRACK_COLOR_ATTR) {
											gpxAppearanceParams.put(CURRENT_TRACK_COLOR_ATTR, item.getValue());
										}
									}
									popup.dismiss();
									updateAppearanceTitle(activity, app, trackWidthProp, renderer,
											apprTitleView,
											gpxAppearanceParams.containsKey(CURRENT_TRACK_WIDTH_ATTR) ? gpxAppearanceParams.get(CURRENT_TRACK_WIDTH_ATTR) : prefWidth.get(),
											gpxAppearanceParams.containsKey(CURRENT_TRACK_COLOR_ATTR) ? gpxAppearanceParams.get(CURRENT_TRACK_COLOR_ATTR) : prefColor.get());
								}
							});
							popup.show();
						}
					});
					builder.setCustomTitle(apprTitleView);
				}
			} else {
				builder.setTitle(R.string.show_gpx);
			}
			builder.setPositiveButton(R.string.shared_string_ok, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (gpxAppearanceParams.size() > 0) {
						for (Map.Entry<String, String> entry : gpxAppearanceParams.entrySet()) {
							final OsmandSettings.CommonPreference<String> pref
									= app.getSettings().getCustomRenderProperty(entry.getKey());
							pref.set(entry.getValue());
						}
						if (activity instanceof MapActivity) {
							ConfigureMapMenu.refreshMapComplete((MapActivity) activity);
						}
					}
					GPXFile currentGPX = null;
					//clear all previously selected files before adding new one
					OsmandApplication app = (OsmandApplication) activity.getApplication();
					if (app != null && app.getSelectedGpxHelper() != null) {
						app.getSelectedGpxHelper().clearAllGpxFileToShow();
					}
					if (app != null && showCurrentGpx && adapter.getItem(0).getSelected()) {
						currentGPX = app.getSavingTrackHelper().getCurrentGpx();
					}
					List<String> s = new ArrayList<>();
					for (int i = (showCurrentGpx ? 1 : 0); i < adapter.length(); i++) {
						if (adapter.getItem(i).getSelected()) {
							s.add(list.get(i).getFileName());
						}
					}
					dialog.dismiss();
					loadGPXFileInDifferentThread(activity, callbackWithObject, dir, currentGPX,
							s.toArray(new String[s.size()]));
				}
			});
			builder.setNegativeButton(R.string.shared_string_cancel, null);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
					&& list.size() > 1 || !showCurrentGpx && list.size() > 0) {
				builder.setNeutralButton(R.string.gpx_add_track, null);
			}
		}

		final AlertDialog dlg = builder.create();
		dlg.setCanceledOnTouchOutside(true);
		if (list.size() == 0 || showCurrentGpx && list.size() == 1) {
			final View footerView = activity.getLayoutInflater().inflate(R.layout.no_gpx_files_list_footer, null);
			TextView descTextView = (TextView)footerView.findViewById(R.id.descFolder);
			String descPrefix = app.getString(R.string.gpx_no_tracks_title_folder);
			SpannableString spannableDesc = new SpannableString(descPrefix + ": " + dir.getAbsolutePath());
			spannableDesc.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
					descPrefix.length() + 1, spannableDesc.length(), 0);
			descTextView.setText(spannableDesc);
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
				footerView.findViewById(R.id.button).setVisibility(View.GONE);
			} else {
				footerView.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						addTrack(activity, dlg);
					}
				});
			}
			dlg.getListView().addFooterView(footerView);
		}
		dlg.getListView().setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (multipleChoice) {
					ContextMenuItem item = adapter.getItem(position);
					item.setSelected(!item.getSelected());
					listAdapter.notifyDataSetInvalidated();
				} else {
					dlg.dismiss();
					if (showCurrentGpx && position == 0) {
						callbackWithObject.processResult(null);
					} else {
						String fileName = list.get(position).getFileName();
						SelectedGpxFile selectedGpxFile =
								app.getSelectedGpxHelper().getSelectedFileByName(fileName);
						if (selectedGpxFile != null) {
							callbackWithObject.processResult(new GPXFile[]{selectedGpxFile.getGpxFile()});
						} else {
							loadGPXFileInDifferentThread(activity, callbackWithObject, dir, null, fileName);
						}
					}
				}
			}
		});
		dlg.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				Button addTrackButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
				if (addTrackButton != null) {
					addTrackButton.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							addTrack(activity, dlg);
						}
					});
				}
			}
		});
		dlg.show();
		try {
			dlg.getListView().setFastScrollEnabled(true);
		} catch (Exception e) {
			// java.lang.ClassCastException: com.android.internal.widget.RoundCornerListAdapter
			// Unknown reason but on some devices fail
		}
		return dlg;
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	private static void addTrack(final Activity activity, final AlertDialog dialog) {
		if (activity instanceof MapActivity) {
			final MapActivity mapActivity = (MapActivity) activity;
			ActivityResultListener listener = new ActivityResultListener(OPEN_GPX_DOCUMENT_REQUEST, new OnActivityResultListener() {
				@Override
				public void onResult(int resultCode, Intent resultData) {
					if (resultCode == Activity.RESULT_OK) {
						if (resultData != null) {
							Uri uri = resultData.getData();
							if (mapActivity.getGpxImportHelper().handleGpxImport(uri, false)) {
								dialog.dismiss();
							}
						}
					}
				}
			});

			Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
			//intent.addCategory(Intent.CATEGORY_OPENABLE);
			//intent.setType("application/gpx+xml");
			//intent.setType("text/plain");
			//intent.setType("text/xml");
			intent.setType("*/*");
			mapActivity.registerActivityResultListener(listener);
			activity.startActivityForResult(intent, OPEN_GPX_DOCUMENT_REQUEST);
		}
	}

	private static void updateAppearanceTitle(Activity activity, OsmandApplication app,
											  RenderingRuleProperty trackWidthProp,
											  RenderingRulesStorage renderer,
											  View apprTitleView,
											  String prefWidthValue,
											  String prefColorValue) {
		TextView widthTextView = (TextView) apprTitleView.findViewById(R.id.widthTitle);
		ImageView colorImageView = (ImageView) apprTitleView.findViewById(R.id.colorImage);
		if (Algorithms.isEmpty(prefWidthValue)) {
			widthTextView.setText(SettingsActivity.getStringPropertyValue(activity, trackWidthProp.getDefaultValueDescription()));
		} else {
			widthTextView.setText(SettingsActivity.getStringPropertyValue(activity, prefWidthValue));
		}
		int color = GpxAppearanceAdapter.parseTrackColor(renderer, prefColorValue);
		if (color == -1) {
			colorImageView.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_action_circle));
		} else {
			colorImageView.setImageDrawable(app.getIconsCache().getPaintedIcon(R.drawable.ic_action_circle, color));
		}
	}

	public static List<GPXInfo> getSortedGPXFilesInfoByDate(File dir, boolean absolutePath) {
		final List<GPXInfo> list = new ArrayList<>();
		readGpxDirectory(dir, list, "", absolutePath);
		Collections.sort(list, new Comparator<GPXInfo>() {
			@Override
			public int compare(GPXInfo object1, GPXInfo object2) {
				long lhs = object1.getLastModified();
				long rhs = object2.getLastModified();
				return lhs < rhs ? 1 : (lhs == rhs ? 0 : -1);
			}
		});
		return list;
	}


	public static List<GPXInfo> getSortedGPXFilesInfo(File dir, final List<String> selectedGpxList, boolean absolutePath) {
		final List<GPXInfo> list = new ArrayList<>();
		readGpxDirectory(dir, list, "", absolutePath);
		if (selectedGpxList != null) {
			for (GPXInfo info : list) {
				for (String fileName : selectedGpxList) {
					if (fileName.endsWith(info.getFileName())) {
						info.setSelected(true);
						break;
					}
				}
			}
		}
		Collections.sort(list, new Comparator<GPXInfo>() {
			@Override
			public int compare(GPXInfo i1, GPXInfo i2) {
				int res = i1.isSelected() == i2.isSelected() ? 0 : i1.isSelected() ? -1 : 1;
				if (res != 0) {
					return res;
				}
				return -i1.getFileName().compareTo(i2.getFileName());
			}
		});
		return list;
	}

	private static void readGpxDirectory(File dir, final List<GPXInfo> list, String parent,
										 boolean absolutePath) {
		if (dir != null && dir.canRead()) {
			File[] files = dir.listFiles();
			if (files != null) {
				for (File f : files) {
					if (f.getName().toLowerCase().endsWith(".gpx")) { //$NON-NLS-1$
						list.add(new GPXInfo(absolutePath ? f.getAbsolutePath() :
								parent + f.getName(), f.lastModified(), f.length()));
					} else if (f.isDirectory()) {
						readGpxDirectory(f, list, parent + f.getName() + "/", absolutePath);
					}
				}
			}
		}
	}

	private static void loadGPXFileInDifferentThread(final Activity activity, final CallbackWithObject<GPXFile[]> callbackWithObject,
													 final File dir, final GPXFile currentFile, final String... filename) {
		final ProgressDialog dlg = ProgressDialog.show(activity, activity.getString(R.string.loading_smth, ""),
				activity.getString(R.string.loading_data));
		new Thread(new Runnable() {
			@Override
			public void run() {
				final GPXFile[] result = new GPXFile[filename.length + (currentFile == null ? 0 : 1)];
				int k = 0;
				String w = "";
				if (currentFile != null) {
					result[k++] = currentFile;
				}
				for (String fname : filename) {
					final File f = new File(dir, fname);
					GPXFile res = GPXUtilities.loadGPXFile(activity.getApplication(), f);
					if (res.warning != null && res.warning.length() > 0) {
						w += res.warning + "\n";
					}
					result[k++] = res;
				}
				dlg.dismiss();
				final String warn = w;
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (warn.length() > 0) {
							Toast.makeText(activity, warn, Toast.LENGTH_LONG).show();
						} else {
							callbackWithObject.processResult(result);
						}
					}
				});
			}

		}, "Loading gpx").start(); //$NON-NLS-1$
	}

	public static void setupGPXChart(OsmandApplication ctx, LineChart mChart, int yLabelsCount) {
		OsmandSettings settings = ctx.getSettings();
		boolean light = settings.isLightContent();

		//mChart.setHardwareAccelerationEnabled(true);
		mChart.setTouchEnabled(true);
		mChart.setDragEnabled(true);
		mChart.setScaleEnabled(true);
		mChart.setPinchZoom(true);
		mChart.setScaleYEnabled(false);
		mChart.setAutoScaleMinMaxEnabled(true);
		mChart.setDrawBorders(false);
		mChart.getDescription().setEnabled(false);
		mChart.setMaxVisibleValueCount(10);
		mChart.setMinOffset(0f);

		mChart.setExtraTopOffset(24f);
		mChart.setExtraBottomOffset(16f);

		// create a custom MarkerView (extend MarkerView) and specify the layout
		// to use for it
		GPXMarkerView mv = new GPXMarkerView(mChart.getContext());
		mv.setChartView(mChart); // For bounds control
		mChart.setMarker(mv); // Set the marker to the chart
		mChart.setDrawMarkers(true);

		XAxis xAxis = mChart.getXAxis();
		xAxis.setDrawAxisLine(false);
		xAxis.setDrawGridLines(false);
		xAxis.setPosition(BOTTOM);
		xAxis.setTextColor(light ? mChart.getResources().getColor(R.color.secondary_text_light) : mChart.getResources().getColor(R.color.secondary_text_dark));

		YAxis yAxis = mChart.getAxisLeft();
		yAxis.enableGridDashedLine(10f, 5f, 0f);
		yAxis.setGridColor(ActivityCompat.getColor(mChart.getContext(), R.color.divider_color));
		yAxis.setDrawAxisLine(false);
		yAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
		yAxis.setXOffset(16f);
		yAxis.setYOffset(-6f);
		yAxis.setLabelCount(yLabelsCount);
		yAxis.setTextColor(light ? mChart.getResources().getColor(R.color.secondary_text_light) : mChart.getResources().getColor(R.color.secondary_text_dark));

		yAxis = mChart.getAxisRight();
		yAxis.enableGridDashedLine(10f, 5f, 0f);
		yAxis.setGridColor(ActivityCompat.getColor(mChart.getContext(), R.color.divider_color));
		yAxis.setDrawAxisLine(false);
		yAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
		yAxis.setXOffset(16f);
		yAxis.setYOffset(-6f);
		yAxis.setLabelCount(yLabelsCount);
		yAxis.setTextColor(light ? mChart.getResources().getColor(R.color.secondary_text_light) : mChart.getResources().getColor(R.color.secondary_text_dark));
		yAxis.setEnabled(false);

		Legend legend = mChart.getLegend();
		legend.setEnabled(false);
	}

	private static float setupXAxisDistance(OsmandApplication ctx, XAxis xAxis, float meters) {
		OsmandSettings settings = ctx.getSettings();
		OsmandSettings.MetricsConstants mc = settings.METRIC_SYSTEM.get();
		float divX;

		String format1 = "{0,number,0.#} ";
		String format2 = "{0,number,0.##} ";
		String fmt = null;
		float granularity = 1f;
		int mainUnitStr;
		float mainUnitInMeters;
		if (mc == OsmandSettings.MetricsConstants.KILOMETERS_AND_METERS) {
			mainUnitStr = R.string.km;
			mainUnitInMeters = METERS_IN_KILOMETER;
		} else if (mc == OsmandSettings.MetricsConstants.NAUTICAL_MILES) {
			mainUnitStr = R.string.nm;
			mainUnitInMeters = METERS_IN_ONE_NAUTICALMILE;
		} else {
			mainUnitStr = R.string.mile;
			mainUnitInMeters = METERS_IN_ONE_MILE;
		}
		if (meters > 9.99f * mainUnitInMeters) {
			fmt = format1;
			granularity = .1f;
		}
		if (meters >= 100 * mainUnitInMeters ||
				meters > 9.99f * mainUnitInMeters ||
				meters > 0.999f * mainUnitInMeters ||
				mc == OsmandSettings.MetricsConstants.MILES_AND_FEET && meters > 0.249f * mainUnitInMeters ||
				mc == OsmandSettings.MetricsConstants.MILES_AND_METERS && meters > 0.249f * mainUnitInMeters ||
				mc == OsmandSettings.MetricsConstants.MILES_AND_YARDS && meters > 0.249f * mainUnitInMeters ||
				mc == OsmandSettings.MetricsConstants.NAUTICAL_MILES && meters > 0.99f * mainUnitInMeters) {

			divX = mainUnitInMeters;
			if (fmt == null) {
				fmt = format2;
				granularity = .01f;
			}
		} else {
			fmt = null;
			granularity = 1f;
			if (mc == OsmandSettings.MetricsConstants.KILOMETERS_AND_METERS || mc == OsmandSettings.MetricsConstants.MILES_AND_METERS) {
				divX = 1f;
				mainUnitStr = R.string.m;
			} else if (mc == OsmandSettings.MetricsConstants.MILES_AND_FEET) {
				divX = 1f / FEET_IN_ONE_METER;
				mainUnitStr = R.string.foot;
			} else if (mc == OsmandSettings.MetricsConstants.MILES_AND_YARDS) {
				divX = 1f / YARDS_IN_ONE_METER;
				mainUnitStr = R.string.yard;
			} else {
				divX = 1f;
				mainUnitStr = R.string.m;
			}
		}

		final String formatX = fmt;
		final String mainUnitX = ctx.getString(mainUnitStr);

		xAxis.setGranularity(granularity);
		xAxis.setValueFormatter(new IAxisValueFormatter() {

			@Override
			public String getFormattedValue(float value, AxisBase axis) {
				if (!Algorithms.isEmpty(formatX)) {
					return MessageFormat.format(formatX + mainUnitX, value);
				} else {
					return (int)value + " " + mainUnitX;
				}
			}
		});

		return divX;
	}

	private static float setupXAxisTime(XAxis xAxis, long timeSpan) {

		final boolean useHours = timeSpan / 3600000 > 0;
		xAxis.setGranularity(1f);
		xAxis.setValueFormatter(new IAxisValueFormatter() {

			@Override
			public String getFormattedValue(float value, AxisBase axis) {
				int seconds = (int)value;
				if (useHours) {
					int hours = seconds / (60 * 60);
					int minutes = (seconds / 60) % 60;
					int sec = seconds % 60;
					return hours + ":" + (minutes < 10 ? "0" + minutes : minutes) + ":" + (sec < 10 ? "0" + sec : sec);
				} else {
					int minutes = (seconds / 60) % 60;
					int sec = seconds % 60;
					return (minutes < 10 ? "0" + minutes : minutes) + ":" + (sec < 10 ? "0" + sec : sec);
				}
			}
		});

		return 1f;
	}

	private static List<Entry> calculateElevationArray(GPXTrackAnalysis analysis, GPXDataSetAxisType axisType,
													   float divX, float convEle) {
		List<Entry> values = new ArrayList<>();
		List<Elevation> elevationData = analysis.elevationData;
		float nextX = 0;
		float nextY;
		float elev;
		float prevElevOrig = -80000;
		float prevElev = 0;
		int i = -1;
		int lastIndex = elevationData.size() - 1;
		Entry lastEntry = null;
		float lastXSameY = -1;
		boolean hasSameY = false;
		float x;
		for (Elevation e : elevationData) {
			i++;
			x = axisType == GPXDataSetAxisType.TIME ? e.time : e.distance;
			if (x > 0) {
				nextX += x / divX;
				if (!Float.isNaN(e.elevation)) {
					elev = e.elevation;
					if (prevElevOrig != -80000) {
						if (elev > prevElevOrig) {
							elev -= 1f;
						} else if (prevElevOrig == elev && i < lastIndex) {
							hasSameY = true;
							lastXSameY = nextX;
							continue;
						}
						if (prevElev == elev && i < lastIndex) {
							hasSameY = true;
							lastXSameY = nextX;
							continue;
						}
						if (hasSameY) {
							values.add(new Entry(lastXSameY, lastEntry.getY()));
						}
						hasSameY = false;
					}
					prevElevOrig = e.elevation;
					prevElev = elev;
					nextY = elev * convEle;
					lastEntry = new Entry(nextX, nextY);
					values.add(lastEntry);
				}
			}
		}
		return values;
	}

	public static OrderedLineDataSet createGPXElevationDataSet(OsmandApplication ctx, LineChart mChart,
															   GPXTrackAnalysis analysis,
															   GPXDataSetAxisType axisType,
															   boolean useRightAxis, boolean drawFilled) {
		OsmandSettings settings = ctx.getSettings();
		OsmandSettings.MetricsConstants mc = settings.METRIC_SYSTEM.get();
		boolean useFeet = (mc == OsmandSettings.MetricsConstants.MILES_AND_FEET) || (mc == OsmandSettings.MetricsConstants.MILES_AND_YARDS);
		boolean light = settings.isLightContent();
		final float convEle = useFeet ? 3.28084f : 1.0f;

		float divX;
		XAxis xAxis = mChart.getXAxis();
		if (axisType == GPXDataSetAxisType.TIME && analysis.isTimeSpecified()) {
			divX = setupXAxisTime(xAxis, analysis.timeSpan);
		} else {
			divX = setupXAxisDistance(ctx, xAxis, analysis.totalDistance);
		}

		final String mainUnitY = useFeet ? ctx.getString(R.string.foot) : ctx.getString(R.string.m);

		YAxis yAxis;
		if (useRightAxis) {
			yAxis = mChart.getAxisRight();
			yAxis.setEnabled(true);
		} else {
			yAxis = mChart.getAxisLeft();
		}
		yAxis.setTextColor(ActivityCompat.getColor(mChart.getContext(), R.color.gpx_chart_blue));
		yAxis.setGridColor(ActivityCompat.getColor(mChart.getContext(), R.color.gpx_chart_blue_grid));
		yAxis.setGranularity(1f);
		yAxis.resetAxisMinimum();
		yAxis.setValueFormatter(new IAxisValueFormatter() {

			@Override
			public String getFormattedValue(float value, AxisBase axis) {
				return (int)value + " " + mainUnitY;
			}
		});

		List<Entry> values = calculateElevationArray(analysis, axisType, divX, convEle);

		OrderedLineDataSet dataSet = new OrderedLineDataSet(values, "", GPXDataSetType.ALTITUDE, axisType);
		dataSet.priority = (float) (analysis.avgElevation - analysis.minElevation) * convEle;
		dataSet.divX = divX;
		dataSet.mulY = convEle;
		dataSet.divY = Float.NaN;
		dataSet.units = mainUnitY;

		dataSet.setColor(ContextCompat.getColor(mChart.getContext(), R.color.gpx_chart_blue));
		dataSet.setLineWidth(1f);
		if (drawFilled) {
			dataSet.setFillAlpha(128);
			dataSet.setFillColor(ContextCompat.getColor(mChart.getContext(), R.color.gpx_chart_blue));
			dataSet.setDrawFilled(true);
		} else {
			dataSet.setDrawFilled(false);
		}

		dataSet.setDrawValues(false);
		dataSet.setValueTextSize(9f);
		dataSet.setFormLineWidth(1f);
		dataSet.setFormSize(15.f);

		dataSet.setDrawCircles(false);
		dataSet.setDrawCircleHole(false);

		dataSet.setHighlightEnabled(true);
		dataSet.setDrawVerticalHighlightIndicator(true);
		dataSet.setDrawHorizontalHighlightIndicator(false);
		dataSet.setHighLightColor(light ? mChart.getResources().getColor(R.color.secondary_text_light) : mChart.getResources().getColor(R.color.secondary_text_dark));

		//dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);

		dataSet.setFillFormatter(new IFillFormatter() {
			@Override
			public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
				return dataProvider.getYChartMin();
			}
		});
		if (useRightAxis) {
			dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
		}
		return dataSet;
	}

	public static OrderedLineDataSet createGPXSpeedDataSet(OsmandApplication ctx, LineChart mChart,
														   GPXTrackAnalysis analysis,
														   GPXDataSetAxisType axisType,
														   boolean useRightAxis, boolean drawFilled) {
		OsmandSettings settings = ctx.getSettings();
		boolean light = settings.isLightContent();

		float divX;
		XAxis xAxis = mChart.getXAxis();
		if (axisType == GPXDataSetAxisType.TIME && analysis.isTimeSpecified()) {
			divX = setupXAxisTime(xAxis, analysis.timeSpan);
		} else {
			divX = setupXAxisDistance(ctx, xAxis, analysis.totalDistance);
		}

		OsmandSettings.SpeedConstants sps = settings.SPEED_SYSTEM.get();
		float mulSpeed = Float.NaN;
		float divSpeed = Float.NaN;
		final String mainUnitY = sps.toShortString(ctx);
		if (sps == OsmandSettings.SpeedConstants.KILOMETERS_PER_HOUR) {
			mulSpeed = 3.6f;
		} else if (sps == OsmandSettings.SpeedConstants.MILES_PER_HOUR) {
			mulSpeed = 3.6f * METERS_IN_KILOMETER / METERS_IN_ONE_MILE;
		} else if (sps == OsmandSettings.SpeedConstants.NAUTICALMILES_PER_HOUR) {
			mulSpeed = 3.6f * METERS_IN_KILOMETER / METERS_IN_ONE_NAUTICALMILE;
		} else if (sps == OsmandSettings.SpeedConstants.MINUTES_PER_KILOMETER) {
			divSpeed = METERS_IN_KILOMETER / 60;
		} else if (sps == OsmandSettings.SpeedConstants.MINUTES_PER_MILE) {
			divSpeed = METERS_IN_ONE_MILE / 60;
		} else {
			mulSpeed = 1f;
		}

		YAxis yAxis;
		if (useRightAxis) {
			yAxis = mChart.getAxisRight();
			yAxis.setEnabled(true);
		} else {
			yAxis = mChart.getAxisLeft();
		}
		yAxis.setTextColor(ActivityCompat.getColor(mChart.getContext(), R.color.gpx_chart_orange));
		yAxis.setGridColor(ActivityCompat.getColor(mChart.getContext(), R.color.gpx_chart_orange_grid));
		yAxis.setAxisMinimum(0f);
		yAxis.setValueFormatter(new IAxisValueFormatter() {

			@Override
			public String getFormattedValue(float value, AxisBase axis) {
				return (int)value + " " + mainUnitY;
			}
		});

		ArrayList<Entry> values = new ArrayList<>();
		List<Speed> speedData = analysis.speedData;
		float nextX = 0;
		float nextY;
		float x;
		for (Speed s : speedData) {
			x = axisType == GPXDataSetAxisType.TIME ? s.time : s.distance;
			if (x > 0) {
				if (axisType == GPXDataSetAxisType.TIME && x > 60) {
					values.add(new Entry(nextX + 1, 0));
					values.add(new Entry(nextX + x - 1, 0));
				}
				nextX += x / divX;
				if (Float.isNaN(divSpeed)) {
					nextY = s.speed * mulSpeed;
				} else {
					nextY = divSpeed / s.speed;
				}
				if (nextY < 0) {
					nextY = 0;
				}
				values.add(new Entry(nextX, nextY));
			}
		}

		OrderedLineDataSet dataSet = new OrderedLineDataSet(values, "", GPXDataSetType.SPEED, axisType);

		if (Float.isNaN(divSpeed)) {
			dataSet.priority = analysis.avgSpeed * mulSpeed;
		} else {
			dataSet.priority = divSpeed / analysis.avgSpeed;
		}
		dataSet.divX = divX;
		if (Float.isNaN(divSpeed)) {
			dataSet.mulY = mulSpeed;
			dataSet.divY = Float.NaN;
		} else {
			dataSet.divY = divSpeed;
			dataSet.mulY = Float.NaN;
		}
		dataSet.units = mainUnitY;

		dataSet.setColor(ContextCompat.getColor(mChart.getContext(), R.color.gpx_chart_orange));
		dataSet.setLineWidth(1f);
		if (drawFilled) {
			dataSet.setFillAlpha(128);
			dataSet.setFillColor(ContextCompat.getColor(mChart.getContext(), R.color.gpx_chart_orange));
			dataSet.setDrawFilled(true);
		} else {
			dataSet.setDrawFilled(false);
		}
		dataSet.setDrawValues(false);
		dataSet.setValueTextSize(9f);
		dataSet.setFormLineWidth(1f);
		dataSet.setFormSize(15.f);

		dataSet.setDrawCircles(false);
		dataSet.setDrawCircleHole(false);

		dataSet.setHighlightEnabled(true);
		dataSet.setDrawVerticalHighlightIndicator(true);
		dataSet.setDrawHorizontalHighlightIndicator(false);
		dataSet.setHighLightColor(light ? mChart.getResources().getColor(R.color.secondary_text_light) : mChart.getResources().getColor(R.color.secondary_text_dark));

		//dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);

		if (useRightAxis) {
			dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
		}
		return dataSet;
	}

	public static OrderedLineDataSet createGPXSlopeDataSet(OsmandApplication ctx, LineChart mChart,
														   GPXTrackAnalysis analysis,
														   GPXDataSetAxisType axisType,
														   List<Entry> eleValues,
														   boolean useRightAxis, boolean drawFilled) {
		OsmandSettings settings = ctx.getSettings();
		boolean light = settings.isLightContent();
		OsmandSettings.MetricsConstants mc = settings.METRIC_SYSTEM.get();
		boolean useFeet = (mc == OsmandSettings.MetricsConstants.MILES_AND_FEET) || (mc == OsmandSettings.MetricsConstants.MILES_AND_YARDS);
		final float convEle = useFeet ? 3.28084f : 1.0f;
		final float totalDistance = analysis.totalDistance;

		float divX;
		XAxis xAxis = mChart.getXAxis();
		if (axisType == GPXDataSetAxisType.TIME && analysis.isTimeSpecified()) {
			divX = setupXAxisTime(xAxis, analysis.timeSpan);
		} else {
			divX = setupXAxisDistance(ctx, xAxis, analysis.totalDistance);
		}

		final String mainUnitY = "%";

		YAxis yAxis;
		if (useRightAxis) {
			yAxis = mChart.getAxisRight();
			yAxis.setEnabled(true);
		} else {
			yAxis = mChart.getAxisLeft();
		}
		yAxis.setTextColor(ActivityCompat.getColor(mChart.getContext(), R.color.gpx_chart_green));
		yAxis.setGridColor(ActivityCompat.getColor(mChart.getContext(), R.color.gpx_chart_green_grid));
		yAxis.setGranularity(1f);
		yAxis.resetAxisMinimum();
		yAxis.setValueFormatter(new IAxisValueFormatter() {

			@Override
			public String getFormattedValue(float value, AxisBase axis) {
				return (int)value + " " + mainUnitY;
			}
		});

		List<Entry> values;
		if (eleValues == null || axisType == GPXDataSetAxisType.TIME) {
			values = calculateElevationArray(analysis, GPXDataSetAxisType.DISTANCE, 1f, 1f);
		} else {
			values = new ArrayList<>(eleValues.size());
			for (Entry e : eleValues) {
				values.add(new Entry(e.getX() * divX, e.getY() / convEle));
			}
		}

		if (values == null) {
			return null;
		}

		int lastIndex = values.size() - 1;

		double STEP = 5;

		float timeDistKoef = 1f;
		if (axisType == GPXDataSetAxisType.TIME) {
			timeDistKoef = analysis.timeSpan / totalDistance / 1000;
			divX = 1f;
		}

		double[] calculatedDist = new double[(int) (totalDistance / STEP) + 1];
		double[] calculatedH = new double[(int) (totalDistance / STEP) + 1];
		int nextW = 0;
		for (int k = 0; k < calculatedDist.length; k++) {
			if (k > 0) {
				calculatedDist[k] = calculatedDist[k - 1] + STEP;
			}
			while(nextW < lastIndex && calculatedDist[k] > values.get(nextW).getX()) {
				nextW ++;
			}
			double pd = nextW == 0 ? 0 : values.get(nextW - 1).getX();
			double ph = nextW == 0 ? values.get(0).getY() : values.get(nextW - 1).getY();
			calculatedH[k] = ph + (values.get(nextW).getY() - ph) / (values.get(nextW).getX() - pd) * (calculatedDist[k] - pd);
		}

		double SLOPE_PROXIMITY = 150;

		double[] calculatedSlopeDist = new double[(int) ((totalDistance - SLOPE_PROXIMITY) / STEP) + 1];
		double[] calculatedSlope = new double[(int) ((totalDistance - SLOPE_PROXIMITY) / STEP) + 1];

		int index = (int) ((SLOPE_PROXIMITY / STEP) / 2);
		for (int k = 0; k < calculatedSlopeDist.length; k++) {
			calculatedSlopeDist[k] = calculatedDist[index + k] * timeDistKoef;
			calculatedSlope[k] = (calculatedH[ 2 * index + k] - calculatedH[k]) * 100 / SLOPE_PROXIMITY;
			if (Double.isNaN(calculatedSlope[k])) {
				calculatedSlope[k] = 0;
			}
		}

		List<Entry> slopeValues = new ArrayList<>(calculatedSlopeDist.length);
		float prevSlope = -80000;
		float slope;
		float x;
		float lastXSameY = 0;
		boolean hasSameY = false;
		Entry lastEntry = null;
		lastIndex = calculatedSlopeDist.length - 1;
		for (int i = 0; i < calculatedSlopeDist.length; i++) {
			x = (float) calculatedSlopeDist[i] / divX;
			slope = (float) calculatedSlope[i];
			if (prevSlope != -80000) {
				if (prevSlope == slope && i < lastIndex) {
					hasSameY = true;
					lastXSameY = x;
					continue;
				}
				if (hasSameY) {
					slopeValues.add(new Entry(lastXSameY, lastEntry.getY()));
				}
				hasSameY = false;
			}
			prevSlope = slope;
			lastEntry = new Entry(x, slope);
			slopeValues.add(lastEntry);
		}

		OrderedLineDataSet dataSet = new OrderedLineDataSet(slopeValues, "", GPXDataSetType.SLOPE, axisType);
		dataSet.divX = divX;
		dataSet.units = mainUnitY;

		dataSet.setColor(ContextCompat.getColor(mChart.getContext(), R.color.gpx_chart_green));
		dataSet.setLineWidth(1f);
		if (drawFilled) {
			dataSet.setFillAlpha(128);
			dataSet.setFillColor(ContextCompat.getColor(mChart.getContext(), R.color.gpx_chart_green));
			dataSet.setDrawFilled(true);
		} else {
			dataSet.setDrawFilled(false);
		}

		dataSet.setDrawValues(false);
		dataSet.setValueTextSize(9f);
		dataSet.setFormLineWidth(1f);
		dataSet.setFormSize(15.f);

		dataSet.setDrawCircles(false);
		dataSet.setDrawCircleHole(false);

		dataSet.setHighlightEnabled(true);
		dataSet.setDrawVerticalHighlightIndicator(true);
		dataSet.setDrawHorizontalHighlightIndicator(false);
		dataSet.setHighLightColor(light ? mChart.getResources().getColor(R.color.secondary_text_light) : mChart.getResources().getColor(R.color.secondary_text_dark));

		//dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);

		/*
		dataSet.setFillFormatter(new IFillFormatter() {
			@Override
			public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
				return dataProvider.getYChartMin();
			}
		});
		*/
		if (useRightAxis) {
			dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
		}
		return dataSet;
	}

	public enum GPXDataSetType {
		ALTITUDE(R.string.altitude, R.drawable.ic_action_altitude_average),
		SPEED(R.string.map_widget_speed, R.drawable.ic_action_speed),
		SLOPE(R.string.shared_string_slope, R.drawable.ic_action_altitude_ascent);

		private int stringId;
		private int imageId;

		private GPXDataSetType(int stringId, int imageId) {
			this.stringId = stringId;
			this.imageId = imageId;
		}

		public String getName(Context ctx) {
			return ctx.getString(stringId);
		}

		public int getStringId() {
			return stringId;
		}

		public int getImageId() {
			return imageId;
		}

		public Drawable getImageDrawable(OsmandApplication app) {
			return app.getIconsCache().getThemedIcon(imageId);
		}
	}

	public enum GPXDataSetAxisType {
		DISTANCE(R.string.distance, R.drawable.ic_action_marker_dark),
		TIME(R.string.shared_string_time, R.drawable.ic_action_time);

		private int stringId;
		private int imageId;

		private GPXDataSetAxisType(int stringId, int imageId) {
			this.stringId = stringId;
			this.imageId = imageId;
		}

		public String getName(Context ctx) {
			return ctx.getString(stringId);
		}

		public int getStringId() {
			return stringId;
		}

		public int getImageId() {
			return imageId;
		}

		public Drawable getImageDrawable(OsmandApplication app) {
			return app.getIconsCache().getThemedIcon(imageId);
		}
	}

	public static class OrderedLineDataSet extends LineDataSet {

		private GPXDataSetType dataSetType;
		private GPXDataSetAxisType dataSetAxisType;

		float priority;
		String units;
		float divX = 1f;
		float divY = 1f;
		float mulY = 1f;

		OrderedLineDataSet(List<Entry> yVals, String label, GPXDataSetType dataSetType, GPXDataSetAxisType dataSetAxisType) {
			super(yVals, label);
			this.dataSetType = dataSetType;
			this.dataSetAxisType = dataSetAxisType;
		}

		public GPXDataSetType getDataSetType() {
			return dataSetType;
		}

		public GPXDataSetAxisType getDataSetAxisType() {
			return dataSetAxisType;
		}

		public float getPriority() {
			return priority;
		}

		public float getDivX() {
			return divX;
		}

		public float getDivY() {
			return divY;
		}

		public float getMulY() {
			return mulY;
		}

		public String getUnits() {
			return units;
		}
	}

	@SuppressLint("ViewConstructor")
	private static class GPXMarkerView extends MarkerView {

		private View textAltView;
		private View textSpdView;
		private View textSlpView;

		public GPXMarkerView(Context context) {
			super(context, R.layout.chart_marker_view);
			textAltView = findViewById(R.id.text_alt_container);
			textSpdView = findViewById(R.id.text_spd_container);
			textSlpView = findViewById(R.id.text_slp_container);
		}

		// callbacks everytime the MarkerView is redrawn, can be used to update the
		// content (user-interface)
		@Override
		public void refreshContent(Entry e, Highlight highlight) {
			ChartData chartData = getChartView().getData();
			if (chartData.getDataSetCount() == 1) {
				OrderedLineDataSet dataSet = (OrderedLineDataSet) chartData.getDataSetByIndex(0);
				String value = Integer.toString((int) e.getY()) + " ";
				String units = dataSet.units;
				switch (dataSet.getDataSetType()) {
					case ALTITUDE:
						((TextView) textAltView.findViewById(R.id.text_alt_value)).setText(value);
						((TextView) textAltView.findViewById(R.id.text_alt_units)).setText(units);
						textAltView.setVisibility(VISIBLE);
						textSpdView.setVisibility(GONE);
						textSlpView.setVisibility(GONE);
						break;
					case SPEED:
						((TextView) textSpdView.findViewById(R.id.text_spd_value)).setText(value);
						((TextView) textSpdView.findViewById(R.id.text_spd_units)).setText(units);
						textAltView.setVisibility(GONE);
						textSpdView.setVisibility(VISIBLE);
						textSlpView.setVisibility(GONE);
						break;
					case SLOPE:
						((TextView) textSlpView.findViewById(R.id.text_slp_value)).setText(value);
						textAltView.setVisibility(GONE);
						textSpdView.setVisibility(GONE);
						textSlpView.setVisibility(VISIBLE);
						break;
				}
				findViewById(R.id.divider).setVisibility(GONE);
			} else if (chartData.getDataSetCount() == 2) {
				OrderedLineDataSet dataSet1 = (OrderedLineDataSet) chartData.getDataSetByIndex(0);
				OrderedLineDataSet dataSet2 = (OrderedLineDataSet) chartData.getDataSetByIndex(1);
				int altSetIndex = -1;
				int spdSetIndex = -1;
				int slpSetIndex = -1;
				switch (dataSet1.getDataSetType()) {
					case ALTITUDE:
						altSetIndex = 0;
						break;
					case SPEED:
						spdSetIndex = 0;
						break;
					case SLOPE:
						slpSetIndex = 0;
						break;
				}
				switch (dataSet2.getDataSetType()) {
					case ALTITUDE:
						altSetIndex = 1;
						break;
					case SPEED:
						spdSetIndex = 1;
						break;
					case SLOPE:
						slpSetIndex = 1;
						break;
				}
				if (altSetIndex != -1) {
					float y = getInterpolatedY(altSetIndex == 0 ? dataSet1 : dataSet2, e);
					((TextView) textAltView.findViewById(R.id.text_alt_value)).setText(Integer.toString((int) y) + " ");
					((TextView) textAltView.findViewById(R.id.text_alt_units)).setText((altSetIndex == 0 ? dataSet1.units : dataSet2.units));
					textAltView.setVisibility(VISIBLE);
				} else {
					textAltView.setVisibility(GONE);
				}
				if (spdSetIndex != -1) {
					float y = getInterpolatedY(spdSetIndex == 0 ? dataSet1 : dataSet2, e);
					((TextView) textSpdView.findViewById(R.id.text_spd_value)).setText(Integer.toString((int) y) + " ");
					((TextView) textSpdView.findViewById(R.id.text_spd_units)).setText(spdSetIndex == 0 ? dataSet1.units : dataSet2.units);
					textSpdView.setVisibility(VISIBLE);
				} else {
					textSpdView.setVisibility(GONE);
				}
				if (slpSetIndex != -1) {
					float y = getInterpolatedY(slpSetIndex == 0 ? dataSet1 : dataSet2, e);
					((TextView) textSlpView.findViewById(R.id.text_slp_value)).setText(Integer.toString((int) y) + " ");
					textSlpView.setVisibility(VISIBLE);
				} else {
					textSlpView.setVisibility(GONE);
				}
				findViewById(R.id.divider).setVisibility(VISIBLE);
			} else {
				textAltView.setVisibility(GONE);
				textSpdView.setVisibility(GONE);
				textSlpView.setVisibility(GONE);
				findViewById(R.id.divider).setVisibility(GONE);
			}
			super.refreshContent(e, highlight);
		}

		private float getInterpolatedY(OrderedLineDataSet ds, Entry e) {
			if (ds.getEntryIndex(e) == -1) {
				Entry upEntry = ds.getEntryForXValue(e.getX(), Float.NaN, DataSet.Rounding.UP);
				Entry downEntry = upEntry;
				int upIndex = ds.getEntryIndex(upEntry);
				if (upIndex > 0) {
					downEntry = ds.getEntryForIndex(upIndex - 1);
				}
				return MapUtils.getInterpolatedY(downEntry.getX(), downEntry.getY(), upEntry.getX(), upEntry.getY(), e.getX());
			} else {
				return e.getY();
			}
		}

		@Override
		public MPPointF getOffset() {
			if (getChartView().getData().getDataSetCount() == 2) {
				int x = findViewById(R.id.divider).getLeft();
				return new MPPointF(-x - AndroidUtils.dpToPx(getContext(), .5f), 0);
			} else {
				return new MPPointF(-getWidth() / 2f, 0);
			}
		}

		@Override
		public MPPointF getOffsetForDrawingAtPoint(float posX, float posY) {
			int margin = AndroidUtils.dpToPx(getContext(), 3f);
			MPPointF offset = getOffset();
			offset.y = -posY;
			if (posX + offset.x - margin < 0) {
				offset.x -= (offset.x + posX - margin);
			}
			if (posX + offset.x + getWidth() + margin > getChartView().getWidth()) {
				offset.x -= (getWidth() - (getChartView().getWidth() - posX) + offset.x) + margin;
			}
			return offset;
		}
	}

	public static class GPXInfo {
		private String fileName;
		private long lastModified;
		private long fileSize;
		private boolean selected;

		GPXInfo(String fileName, long lastModified, long fileSize) {
			this.fileName = fileName;
			this.lastModified = lastModified;
			this.fileSize = fileSize;
		}

		public String getFileName() {
			return fileName;
		}

		public long getLastModified() {
			return lastModified;
		}

		public long getFileSize() {
			return fileSize;
		}

		public boolean isSelected() {
			return selected;
		}

		public void setSelected(boolean selected) {
			this.selected = selected;
		}
	}
}
