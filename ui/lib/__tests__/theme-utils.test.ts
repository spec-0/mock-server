/**
 * Quick verification tests for theme utility functions
 * 
 * Run with: npx tsx lib/__tests__/theme-utils.test.ts
 * Or use in your test suite
 */

import { getColor, getCSSVar, getMUIColor, hexToHSL } from '../theme-config';

// Simple test runner
function test(name: string, fn: () => boolean) {
  try {
    const result = fn();
    if (result) {
      console.log(`✅ ${name}`);
    } else {
      console.error(`❌ ${name} - Test failed`);
    }
  } catch (error) {
    console.error(`❌ ${name} - Error:`, error);
  }
}

console.log('🧪 Testing Theme Utility Functions\n');

// Test getColor
test('getColor - primary 500', () => {
  return getColor('primary', 500) === '#6366F1';
});

test('getColor - primary main', () => {
  return getColor('primary', 'main') === '#6366F1';
});

test('getColor - success main', () => {
  return getColor('success', 'main') === '#10B981';
});

test('getColor - grey 500', () => {
  return getColor('grey', 500) === '#6B7280';
});

test('getColor - default to main when no shade', () => {
  return getColor('primary') === '#6366F1';
});

// Test getCSSVar
test('getCSSVar - primary 500', () => {
  return getCSSVar('primary', 500) === 'hsl(var(--primary-500))';
});

test('getCSSVar - success 600', () => {
  return getCSSVar('success', 600) === 'hsl(var(--success-600))';
});

// Test getMUIColor
test('getMUIColor - primary main', () => {
  return getMUIColor('primary', 'main') === 'primary.main';
});

test('getMUIColor - success light', () => {
  return getMUIColor('success', 'light') === 'success.light';
});

test('getMUIColor - default to main', () => {
  return getMUIColor('primary') === 'primary.main';
});

// Test hexToHSL
test('hexToHSL - primary color', () => {
  const result = hexToHSL('#6366F1');
  return result.includes('239') && result.includes('%');
});

test('hexToHSL - success color', () => {
  const result = hexToHSL('#10B981');
  return result.includes('160') && result.includes('%');
});

test('hexToHSL - handles hex without #', () => {
  const result = hexToHSL('6366F1');
  return result.includes('239') && result.includes('%');
});

console.log('\n✨ All tests completed!');

