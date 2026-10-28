package org.odm.gtk4;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.gnome.gtk.*;
import org.manager.download.Download;
import org.manager.util.SizeUnits;
import org.ytdlp.YtDlpSettings;

/** Executed in a fresh JVM by I18nTest with a real Linux locale environment. */
public final class I18nLocaleProbe {
    // Independent expectations exercise actual gettext and GtkBuilder startup,
    // including contextual labels, reordered arguments and CJK plural rules.
    private static final List<String> LANGUAGES = List.of("fr", "de", "es", "ja", "pt", "zh");
    private static final Map<String, List<String>> TEXT = Map.ofEntries(
            Map.entry("Preferences", List.of("Préférences", "Einstellungen", "Preferencias", "設定", "Preferências", "首选项")),
            Map.entry("Active", List.of("Actif", "Aktiv", "Activo", "稼働中", "Ativo", "运行中")),
            Map.entry("0 items", List.of("0 élément", "0 Elemente", "0 elementos", "0 項目", "0 itens", "0 项")),
            Map.entry("1 item", List.of("1 élément", "1 Element", "1 elemento", "1 項目", "1 item", "1 项")),
            Map.entry("2 items", List.of("2 éléments", "2 Elemente", "2 elementos", "2 項目", "2 itens", "2 项")),
            Map.entry("1 of 2 items selected", List.of("1 sur 2 éléments sélectionnés", "1 von 2 Elementen ausgewählt",
                    "1 de 2 elementos seleccionados", "2 項目中 1 項目を選択", "1 de 2 itens selecionados", "已选择 2 项中的 1 项")),
            Map.entry("MB", List.of("Mo", "MB", "MB", "MB", "MB", "MB")),
            Map.entry("Media", List.of("Média", "Medien", "Multimedia", "メディア", "Multimédia", "媒体")),
            Map.entry("Preserve native formats", List.of("Conserver les formats natifs", "Originalformate beibehalten",
                    "Conservar formatos originales", "元の形式を保持", "Preservar formatos originais", "保留原始格式")),
            Map.entry("High", List.of("Haute", "Hoch", "Alta", "高", "Alta", "高")),
            Map.entry("Copy URLs", List.of("Copier les URL", "URLs kopieren", "Copiar URL", "URL をコピー", "Copiar URLs", "复制 URL")),
            Map.entry("2 URLs copied", List.of("2 URL copiées", "2 URLs kopiert", "2 URL copiadas", "2 件の URL をコピーしました", "2 URLs copiados", "已复制 2 个 URL")),
            Map.entry("Open Download Manager - Settings", List.of("Open Download Manager - Paramètres", "Open Download Manager - Einstellungen",
                    "Open Download Manager - Configuración", "Open Download Manager - 設定", "Open Download Manager - Definições", "Open Download Manager - 设置")),
            Map.entry("Download", List.of("Téléchargement", "Download", "Descarga", "ダウンロード", "Transferência", "下载")),
            Map.entry("Speed: —", List.of("Vitesse : —", "Geschwindigkeit: —", "Velocidad: —", "速度：—", "Velocidade: —", "速度：—")),
            Map.entry("Max speed: —", List.of("Vitesse maximale : —", "Max. Geschwindigkeit: —", "Velocidad máx.: —", "最大速度：—", "Velocidade máx.: —", "最高速度：—")),
            Map.entry("1,5 MB", List.of("1,5 Mo", "1,5 MB", "1,5 MB", "1,5 MB", "1,5 MB", "1,5 MB")),
            Map.entry("All Status", List.of("Tous les états", "Alle Status", "Todos los estados", "すべての状態", "Todos os estados", "所有状态")));

    private static String text(String language, String source) {
        return language.equals("en") ? source : TEXT.get(source).get(LANGUAGES.indexOf(language));
    }

    public static void main(String[] args) {
        try {
            verify(args[0]);
            System.out.println("LOCALE CHECK PASSED");
            System.exit(0);
        } catch (Throwable error) {
            error.printStackTrace();
            System.exit(1);
        }
    }

    private static void verify(String language) throws Exception {
        I18n.initialize();
        equal(language, org.manager.util.SystemLocale.display().getLanguage());
        equal(text(language, "Preferences"), I18n.tr("Preferences"));
        equal("A missing catalog message", I18n.tr("A missing catalog message"));
        equal("A missing catalog message", I18n.context("missing", "A missing catalog message"));
        equal(text(language, "Active"), I18n.context("mirror-state", "Active"));
        equal("", I18n.tr(""));
        equal(text(language, "0 items"), I18n.plural("%d item", "%d items", 0));
        equal(text(language, "1 item"), I18n.plural("%d item", "%d items", 1));
        equal(text(language, "2 items"), I18n.plural("%d item", "%d items", 2));
        equal(text(language, "1 of 2 items selected"),
                I18n.plural("%2$d of %1$d item selected", "%2$d of %1$d items selected", 2, 1));
        // The total selects the plural form in every language.
        equal(text(language, "MB"), SizeUnits.current().get(2));
        equal(text(language, "Media"), ImportEngine.YT_DLP.label());
        equal(Download.Type.YOUTUBE, ImportEngine.YT_DLP.type());
        equal("mp4-compatible", YtDlpSettings.ContainerProfile.MP4_COMPATIBLE.settingValue());
        equal(text(language, "Preserve native formats"),
                MediaPresentation.container(YtDlpSettings.ContainerProfile.PRESERVE_NATIVE));
        equal("High", FileTreeSupport.normalizePriority(text(language, "High")));
        var copy = new DownloadLinkCopy(List.of("https://example.test/1", "https://example.test/2"));
        equal(text(language, "Copy URLs"), copy.label());
        equal(text(language, "2 URLs copied"), copy.confirmation());

        Class.forName("org.gnome.glib.GLib");
        Class.forName("org.gnome.glib.MainContext");
        // This probe has no GtkApplication.run() loop. Own its context until
        // process exit so Java-GI cleanup is queued to the GTK thread instead
        // of invoking native widget destruction directly on a Cleaner thread.
        if (!org.gnome.glib.MainContext.default_().acquire()) {
            throw new AssertionError("Could not own the GTK main context");
        }
        Gtk.init();
        GtkBuilder main = null;
        GtkBuilder newDownload = null;
        var builders = new java.util.ArrayList<GtkBuilder>();
        var windows = new java.util.ArrayList<Window>();
        for (String ui : List.of("about", "action-output", "completion-command", "import-list",
                "import-remote", "import-sequence", "jackett-settings", "main-window", "network-options",
                "new-download", "new-media", "new-website", "property", "search-torrents", "settings",
                "sources", "start-shutdown")) {
            GtkBuilder builder = UiLoader.load("/ui/" + ui + ".ui");
            builders.add(builder);
            for (var object : UiLoader.objects(builder)) {
                if (object instanceof Window window) { windows.add(window); }
            }
            if (ui.equals("settings")) {
                equal(text(language, "Open Download Manager - Settings"),
                        Widgets.require(builder, "settings_dialog", Window.class).getTitle());
            }
            if (ui.equals("main-window")) { main = builder; }
            if (ui.equals("new-download")) { newDownload = builder; }
        }
        try {
            verifyFilters(main, language);
            Notebook tabs = Widgets.require(newDownload, "options_notebook", Notebook.class);
            equal(text(language, "Download"),
                    ((Label) tabs.getTabLabel(tabs.getNthPage(0))).getLabel());
            TreeStore files = Widgets.require(newDownload, "files_liststore", TreeStore.class);
            FileTreeSupport.reconcile(files, new LinkedHashMap<>(), List.of(
                    new FileTreeSupport.Entry(true, "file.bin", 1024, 0, 1, "High")), null);
            TreeIter row = new TreeIter();
            if (!files.getIterFirst(row)) { throw new AssertionError("Missing file row"); }
            equal(text(language, "High"), TreeStoreCells.getString(files, row, FileTreeSupport.PRIORITY_TEXT_COLUMN));
            equal("High", FileTreeSupport.priorities(files).get(1));
            DownloadProgressGraph graph = new DownloadProgressGraph(main);
            try {
                equal(text(language, "Speed: —"),
                        Widgets.require(main, "info_speed_value", Label.class).getLabel());
                Download download = new Download(URI.create("https://example.test/file"));
                download.setStatus(Download.Status.COMPLETED);
                graph.update(download);
                equal(text(language, "Max speed: —"),
                        Widgets.require(main, "info_speed_value", Label.class).getLabel());
            } finally { graph.dispose(); }
            if (Locale.getDefault(Locale.Category.FORMAT).getLanguage().equals("fr")) {
                equal(text(language, "1,5 MB"), DownloadFormats.size(1572864));
                equal("12,50%", ProgressPresentation.percentage(12.5));
            }
        } finally {
            windows.forEach(Window::destroy);
            while (org.gnome.glib.MainContext.default_().iteration(false)) { }
            java.lang.ref.Reference.reachabilityFence(builders);
        }
    }

    private static void verifyFilters(GtkBuilder builder, String language) {
        ListStore statuses = Widgets.require(builder, "status_store", ListStore.class);
        ListStore downloads = Widgets.require(builder, "download_store", ListStore.class);
        DownloadListPresenter presenter = new DownloadListPresenter(statuses,
                Widgets.require(builder, "category_store", ListStore.class), downloads,
                Widgets.require(builder, "global_progress_store", ListStore.class),
                Widgets.require(builder, "status_treeview", TreeView.class),
                Widgets.require(builder, "category_treeview", TreeView.class), () -> { });
        Download queued = new Download(URI.create("https://example.test/queued.bin"));
        queued.setStatus(Download.Status.QUEUED);
        Download finished = new Download(URI.create("https://example.test/finished.bin"));
        finished.setStatus(Download.Status.COMPLETED);
        presenter.refresh(List.of(queued, finished));
        TreeIter status = new TreeIter();
        if (!statuses.getIterFirst(status)) { throw new AssertionError("Missing status filters"); }
        equal(text(language, "All Status"), ListStoreCells.getString(statuses, status, 2));
        int completedFilter = java.util.Arrays.asList(DownloadListPresenter.STATUS_FILTERS).indexOf("Finished");
        equal(true, presenter.selectStatusFilterAt(completedFilter));
        presenter.refresh(List.of(queued, finished));
        equal(1, downloads.iterNChildren(null));
        equal(finished, presenter.rowAt(0));
        presenter.refresh(List.of());
    }

    private static void equal(Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError("Expected [" + expected + "] but was [" + actual + "]");
        }
    }
}
