import java.awt.Color;

/** Colour palettes for Morning, Noon, Evening and Night. */
final class Theme {
    final String name;
    final Color bg, panel, stage, line, text, mute, accent, accent2, accent3, on, mapBg, road, dash, tint;
    final Color[] cars;
    final boolean lights;

    private Theme(String name, String bg, String panel, String stage, String line, String text, String mute,
                  String accent, String accent2, String accent3, String on, String mapBg, String road, String dash,
                  Color tint, boolean lights, String... cars) {
        this.name = name; this.bg = c(bg); this.panel = c(panel); this.stage = c(stage); this.line = c(line);
        this.text = c(text); this.mute = c(mute); this.accent = c(accent); this.accent2 = c(accent2); this.accent3 = c(accent3);
        this.on = c(on); this.mapBg = c(mapBg); this.road = c(road); this.dash = c(dash); this.tint = tint; this.lights = lights;
        this.cars = new Color[cars.length];
        for (int i = 0; i < cars.length; i++) this.cars[i] = c(cars[i]);
    }
    static Color c(String hex) { return new Color(Integer.parseInt(hex, 16)); }

    static final Theme MORNING = new Theme("Morning", "FFF1E0", "FFFAF3", "F3E3CC", "F0D5B2", "3B2619", "8D6E58",
            "F0642F", "E8A02C", "D6457A", "FFFFFF", "F3E3CC", "D8C6AA", "B79F7C", new Color(255, 160, 80, 26), false,
            "C0462A", "6B4A8F", "2D7D6B", "8A5A2B");
    static final Theme NOON = new Theme("Noon", "FFFBE3", "FFFFFF", "F1EBC4", "EFE2A0", "2C2A16", "7B7548",
            "D98A00", "1F9A62", "E0533D", "FFFFFF", "F1EBC4", "D3CCA0", "A59D6C", new Color(255, 235, 120, 26), false,
            "C0392B", "7A3FA0", "1F7A4D", "3A6EA5");
    static final Theme EVENING = new Theme("Evening", "1E1229", "2B1A3A", "241530", "46295C", "FBE8EF", "BB9BB8",
            "FF7A5C", "FFB347", "C46BFF", "2A0F1A", "241530", "3B2549", "6E4A84", new Color(255, 100, 70, 33), true,
            "FFB08A", "E59BD0", "C8A2FF", "FFD27A");
    static final Theme NIGHT = new Theme("Night", "0B0E13", "131822", "0A0D12", "222A38", "E8ECF3", "8B95A7",
            "2EE59D", "FFB933", "C58BFF", "04130C", "080A0E", "161C27", "2C3546", new Color(0, 0, 0, 0), true,
            "C8D1E0", "8AA0C4", "D9B88A", "9BD0B5");
    static final Theme[] ALL = {MORNING, NOON, EVENING, NIGHT};
    static Theme current = NIGHT;

    static Theme forHour(int h) { return h >= 5 && h < 11 ? MORNING : h >= 11 && h < 16 ? NOON : h >= 16 && h < 19 ? EVENING : NIGHT; }
}
