/**
 * Centralized Theme Configuration
 * 
 * This file contains all theme-related configurations for the application.
 * Any changes to colors, fonts, spacing, or other design tokens should be made here.
 * 
 * This ensures consistency across the entire application and makes it easy to
 * implement global theme changes in the future.
 */

export const themeConfig = {
  // Color Palette
  colors: {
    primary: {
      main: '#6366F1',
      light: '#818CF8',
      dark: '#4F46E5',
      50: '#EEF2FF',
      100: '#E0E7FF',
      200: '#C7D2FE',
      300: '#A5B4FC',
      400: '#818CF8',
      500: '#6366F1',
      600: '#4F46E5',
      700: '#4338CA',
      800: '#3730A3',
      900: '#312E81',
    },
    secondary: {
      main: '#9c27b0',
      light: '#ba68c8',
      dark: '#7b1fa2',
    },
    success: {
      main: '#10B981',
      light: '#34D399',
      dark: '#059669',
    },
    warning: {
      main: '#F59E0B',
      light: '#FBBF24',
      dark: '#D97706',
    },
    error: {
      main: '#EF4444',
      light: '#F87171',
      dark: '#DC2626',
    },
    info: {
      main: '#3B82F6',
      light: '#60A5FA',
      dark: '#2563EB',
    },
    grey: {
      50: '#F9FAFB',
      100: '#F3F4F6',
      200: '#E5E7EB',
      300: '#D1D5DB',
      400: '#9CA3AF',
      500: '#6B7280',
      600: '#4B5563',
      700: '#374151',
      800: '#1F2937',
      900: '#111827',
    },
  },

  // Typography
  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    h1: {
      fontSize: '2.5rem',
      fontWeight: 700,
      lineHeight: 1.2,
    },
    h2: {
      fontSize: '2rem',
      fontWeight: 600,
      lineHeight: 1.3,
    },
    h3: {
      fontSize: '1.75rem',
      fontWeight: 600,
      lineHeight: 1.3,
    },
    h4: {
      fontSize: '1.5rem',
      fontWeight: 600,
      lineHeight: 1.4,
    },
    h5: {
      fontSize: '1.25rem',
      fontWeight: 600,
      lineHeight: 1.4,
    },
    h6: {
      fontSize: '1.125rem',
      fontWeight: 600,
      lineHeight: 1.4,
    },
    body1: {
      fontSize: '1rem',
      lineHeight: 1.6,
    },
    body2: {
      fontSize: '0.875rem',
      lineHeight: 1.6,
    },
    caption: {
      fontSize: '0.75rem',
      lineHeight: 1.5,
    },
  },

  // Spacing
  spacing: {
    xs: 4,
    sm: 8,
    md: 16,
    lg: 24,
    xl: 32,
    xxl: 48,
  },

  // Border Radius
  borderRadius: {
    sm: 4,
    md: 8,
    lg: 12,
    xl: 16,
  },

  // Shadows
  shadows: {
    sm: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
    md: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
    lg: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)',
    xl: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
  },

  // Breakpoints
  breakpoints: {
    xs: 0,
    sm: 600,
    md: 900,
    lg: 1200,
    xl: 1536,
  },

  // Component-specific configurations
  components: {
    // Card configurations
    card: {
      borderRadius: 12,
      padding: 24,
      shadow: 'md',
    },
    
    // Button configurations
    button: {
      borderRadius: 8,
      fontWeight: 600,
      textTransform: 'none' as const,
    },
    
    // Input configurations
    input: {
      borderRadius: 8,
      borderWidth: 1,
    },
    
    // Table configurations
    table: {
      headerBackground: 'grey.50',
      rowHover: 'grey.50',
      borderColor: 'grey.200',
    },
  },

  // Dark mode specific overrides
  darkMode: {
    background: {
      default: '#111827',
      paper: '#1F2937',
    },
    text: {
      primary: '#E5E7EB',
      secondary: '#9CA3AF',
    },
    divider: 'rgba(255, 255, 255, 0.12)',
  },

  // Light mode specific overrides
  lightMode: {
    background: {
      default: '#F9FAFB',
      paper: '#FFFFFF',
    },
    text: {
      primary: '#1F2937',
      secondary: '#6B7280',
    },
    divider: 'rgba(0, 0, 0, 0.12)',
  },
};

/**
 * Helper function to get theme-aware colors
 */
export const getThemeColors = (isDark: boolean) => ({
  ...themeConfig.colors,
  background: isDark ? themeConfig.darkMode.background : themeConfig.lightMode.background,
  text: isDark ? themeConfig.darkMode.text : themeConfig.lightMode.text,
  divider: isDark ? themeConfig.darkMode.divider : themeConfig.lightMode.divider,
});

/**
 * Helper function to get component styles
 */
export const getComponentStyles = (component: keyof typeof themeConfig.components) => {
  return themeConfig.components[component];
};

// ============================================================================
// Theme Utility Functions
// ============================================================================

/**
 * Type definitions for color names and shades
 */
type ColorName = keyof typeof themeConfig.colors;
type ColorShade = number | 'main' | 'light' | 'dark';
type ColorWithShades = {
  main: string;
  light: string;
  dark: string;
  [key: number]: string;
};

/**
 * Get theme color by semantic name with type safety
 * 
 * @param colorName - Color name (primary, secondary, success, error, warning, info, grey)
 * @param shade - Optional shade (50-900 for colors with scales, or 'main' | 'light' | 'dark')
 * @returns Color hex value as string
 * 
 * @example
 * ```typescript
 * getColor('primary', 500) // Returns '#6366F1'
 * getColor('primary', 'main') // Returns '#6366F1'
 * getColor('success', 'main') // Returns '#10B981'
 * getColor('grey', 500) // Returns '#6B7280'
 * ```
 */
export const getColor = (
  colorName: ColorName,
  shade?: ColorShade
): string => {
  const color = themeConfig.colors[colorName];
  
  // Handle invalid color name (shouldn't happen with TypeScript, but runtime safety)
  if (!color) {
    console.warn(`[getColor] Color "${colorName}" not found in theme. Falling back to primary.main`);
    return themeConfig.colors.primary.main;
  }
  
  // If color is an object (has main/light/dark and possibly numeric shades)
  if (typeof color === 'object' && color !== null) {
    const colorObj = color as ColorWithShades;
    
    // If no shade specified, return main
    if (shade === undefined) {
      return colorObj.main;
    }
    
    // Handle string variants
    if (shade === 'main') {
      return colorObj.main;
    }
    if (shade === 'light') {
      return colorObj.light;
    }
    if (shade === 'dark') {
      return colorObj.dark;
    }
    
    // Handle numeric shades (50, 100, 200, etc.)
    if (typeof shade === 'number') {
      const shadeValue = colorObj[shade];
      if (shadeValue && typeof shadeValue === 'string') {
        return shadeValue;
      }
      // Fallback to main if numeric shade doesn't exist
      console.warn(
        `[getColor] Shade ${shade} not found for color "${colorName}". Falling back to main.`
      );
      return colorObj.main;
    }
  }
  
  // Fallback (shouldn't reach here with current structure)
  return themeConfig.colors.primary.main;
};

/**
 * Get CSS variable value for Tailwind
 * 
 * @param colorName - Color name (primary, secondary, success, error, warning, info)
 * @param shade - Shade number (50, 100, 200, etc.)
 * @returns CSS variable string like 'hsl(var(--primary-500))'
 * 
 * @example
 * ```typescript
 * getCSSVar('primary', 500) // Returns 'hsl(var(--primary-500))'
 * getCSSVar('success', 600) // Returns 'hsl(var(--success-600))'
 * ```
 * 
 * @note This function generates the CSS variable reference.
 * In practice, you'd typically use Tailwind classes like `bg-primary-500`,
 * but this is useful for inline styles or dynamic color selection.
 */
export const getCSSVar = (
  colorName: 'primary' | 'secondary' | 'success' | 'error' | 'warning' | 'info',
  shade: number
): string => {
  // Validate shade is a valid number
  if (typeof shade !== 'number' || shade < 50 || shade > 900 || shade % 50 !== 0) {
    console.warn(
      `[getCSSVar] Invalid shade ${shade}. Must be a multiple of 50 between 50-900. Using 500.`
    );
    shade = 500;
  }
  
  return `hsl(var(--${colorName}-${shade}))`;
};

/**
 * Get MUI theme color reference string
 * 
 * @param colorName - Color name (primary, secondary, success, error, warning, info)
 * @param variant - Color variant ('main', 'light', 'dark')
 * @returns MUI theme reference string like 'primary.main'
 * 
 * @example
 * ```typescript
 * getMUIColor('primary', 'main') // Returns 'primary.main'
 * getMUIColor('success', 'light') // Returns 'success.light'
 * ```
 * 
 * @note This returns a string that can be used in MUI's sx prop.
 * MUI will resolve it from the theme object.
 */
export const getMUIColor = (
  colorName: 'primary' | 'secondary' | 'success' | 'error' | 'warning' | 'info',
  variant: 'main' | 'light' | 'dark' = 'main'
): string => {
  return `${colorName}.${variant}`;
};

/**
 * Convert hex color to HSL format for CSS variables
 * 
 * @param hex - Hex color string (e.g., '#6366F1' or '6366F1')
 * @returns HSL string in format "h s% l%" (e.g., "239 84.6% 67.1%")
 * 
 * @example
 * ```typescript
 * hexToHSL('#6366F1') // Returns '239 84.6% 67.1%'
 * hexToHSL('#10B981') // Returns '160 84.1% 39.0%'
 * ```
 * 
 * @note This is useful for generating CSS variables programmatically
 * or converting theme colors to HSL format.
 */
export const hexToHSL = (hex: string): string => {
  // Remove # if present and validate
  const cleanHex = hex.replace('#', '').trim();
  
  if (!/^[0-9A-Fa-f]{6}$/.test(cleanHex)) {
    console.warn(`[hexToHSL] Invalid hex color: ${hex}. Returning default.`);
    return '0 0% 0%'; // Black as fallback
  }
  
  // Parse RGB values (0-255)
  const r = parseInt(cleanHex.substring(0, 2), 16) / 255;
  const g = parseInt(cleanHex.substring(2, 4), 16) / 255;
  const b = parseInt(cleanHex.substring(4, 6), 16) / 255;
  
  // Calculate min, max, and delta
  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  const delta = max - min;
  
  let h: number, s: number, l: number;
  
  // Calculate lightness
  l = (max + min) / 2;
  
  // Calculate saturation and hue
  if (delta === 0) {
    // Achromatic (gray)
    h = 0;
    s = 0;
  } else {
    // Calculate saturation
    s = l > 0.5 ? delta / (2 - max - min) : delta / (max + min);
    
    // Calculate hue
    switch (max) {
      case r:
        h = ((g - b) / delta + (g < b ? 6 : 0)) / 6;
        break;
      case g:
        h = ((b - r) / delta + 2) / 6;
        break;
      case b:
        h = ((r - g) / delta + 4) / 6;
        break;
      default:
        h = 0;
    }
  }
  
  // Convert to degrees and percentages, round to 1 decimal
  h = Math.round(h * 360);
  s = Math.round(s * 100 * 10) / 10;
  l = Math.round(l * 100 * 10) / 10;
  
  return `${h} ${s}% ${l}%`;
};

/**
 * CSS Variables for Tailwind integration
 */
export const cssVariables = {
  light: {
    '--background': '249 250 251', // #F9FAFB
    '--foreground': '31 41 55', // #1F2937
    '--card': '255 255 255', // #FFFFFF
    '--card-foreground': '31 41 55', // #1F2937
    '--popover': '255 255 255', // #FFFFFF
    '--popover-foreground': '31 41 55', // #1F2937
    '--primary': '99 102 241', // #6366F1
    '--primary-foreground': '255 255 255', // #FFFFFF
    '--secondary': '243 244 246', // #F3F4F6
    '--secondary-foreground': '31 41 55', // #1F2937
    '--muted': '243 244 246', // #F3F4F6
    '--muted-foreground': '107 114 128', // #6B7280
    '--accent': '243 244 246', // #F3F4F6
    '--accent-foreground': '31 41 55', // #1F2937
    '--destructive': '239 68 68', // #EF4444
    '--destructive-foreground': '255 255 255', // #FFFFFF
    '--border': '229 231 235', // #E5E7EB
    '--input': '229 231 235', // #E5E7EB
    '--ring': '99 102 241', // #6366F1
  },
  dark: {
    '--background': '17 24 39', // #111827
    '--foreground': '229 231 235', // #E5E7EB
    '--card': '31 41 55', // #1F2937
    '--card-foreground': '229 231 235', // #E5E7EB
    '--popover': '31 41 55', // #1F2937
    '--popover-foreground': '229 231 235', // #E5E7EB
    '--primary': '99 102 241', // #6366F1
    '--primary-foreground': '255 255 255', // #FFFFFF
    '--secondary': '55 65 81', // #374151
    '--secondary-foreground': '229 231 235', // #E5E7EB
    '--muted': '55 65 81', // #374151
    '--muted-foreground': '156 163 175', // #9CA3AF
    '--accent': '55 65 81', // #374151
    '--accent-foreground': '229 231 235', // #E5E7EB
    '--destructive': '239 68 68', // #EF4444
    '--destructive-foreground': '255 255 255', // #FFFFFF
    '--border': '55 65 81', // #374151
    '--input': '55 65 81', // #374151
    '--ring': '99 102 241', // #6366F1
  },
};
