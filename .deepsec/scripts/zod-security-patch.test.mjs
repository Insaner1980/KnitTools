import assert from 'node:assert/strict';
import { readFileSync, realpathSync } from 'node:fs';
import { createRequire } from 'node:module';
import { dirname, resolve } from 'node:path';
import { test } from 'node:test';
import { fileURLToPath, pathToFileURL } from 'node:url';

// Exercise Sandbox's actual dependency for GHSA-mrhx-c7xc-m3w5.
const root = fileURLToPath(new URL('../', import.meta.url));
const deepsecRequire = createRequire(realpathSync(resolve(root, 'node_modules/deepsec/package.json')));
const sandboxEntry = deepsecRequire.resolve('@vercel/sandbox');
const sandboxRequire = createRequire(sandboxEntry);
const zodRoot = dirname(sandboxRequire.resolve('zod/package.json'));
const zodPackage = JSON.parse(readFileSync(resolve(zodRoot, 'package.json'), 'utf8'));
const modules = {
  cjs: sandboxRequire('zod'),
  esm: await import(pathToFileURL(resolve(zodRoot, zodPackage.module)).href),
};

test('Sandbox resolves the upstream security-fixed Zod release', () => {
  assert.equal(zodPackage.version, '4.6.7');
});

for (const [format, { z }] of Object.entries(modules)) {
  for (const kind of ['array.max', 'array.length', 'set.max', 'set.size']) {
    for (const async of [false, true]) {
      test(`${format}: ${kind} rejects oversized input before parsing (${async ? 'async' : 'sync'})`, async () => {
        let visits = 0;
        let refinements = 0;
        const element = z.preprocess(value => { visits++; return value; }, z.string());
        const [collection, bound] = kind.split('.');
        const schema = z.object({ values: z[collection](element)[bound](2, 'Too many values') })
          .superRefine(() => { refinements++; });
        const values = collection === 'array' ? [1, 2, 3] : new Set([1, 2, 3]);
        const result = async ? await schema.safeParseAsync({ values }) : schema.safeParse({ values });
        assert.equal(result.success, false);
        assert.equal(visits, 0);
        assert.equal(refinements, 0);
        assert.equal(result.error.issues.length, 1);
        assert.equal(result.error.issues[0].code, 'too_big');
        assert.equal(result.error.issues[0].message, 'Too many values');
        assert.deepEqual(result.error.issues[0].path, ['values']);
      });
    }
  }

  test(`${format}: valid, undersized and unbounded collections retain validation`, () => {
    assert.deepEqual(z.array(z.string()).length(2).parse(['a', 'b']), ['a', 'b']);
    assert.deepEqual(z.set(z.string()).size(2).parse(new Set(['a', 'b'])), new Set(['a', 'b']));
    for (const [schema, value] of [
      [z.array(z.string()).length(2), [1]],
      [z.set(z.string()).size(2), new Set([1])],
    ]) {
      const result = schema.safeParse(value);
      assert.equal(result.success, false);
      assert.deepEqual(result.error.issues.map(issue => issue.code).sort(), ['invalid_type', 'too_small']);
    }
    assert.equal(z.array(z.string()).safeParse(['a', 2]).success, false);
    assert.equal(z.set(z.string()).safeParse(new Set(['a', 2])).success, false);
  });
}

test('Sandbox validator and API remain compatible without network access', async () => {
  const { InjectionRuleValidator } = await import(pathToFileURL(resolve(dirname(sandboxEntry), 'api-client/validators.js')).href);
  assert.equal(InjectionRuleValidator.safeParse({ domain: 'example.test', headers: { 'x-test': 'value' } }).success, true);
  assert.equal(InjectionRuleValidator.safeParse({ domain: 'example.test', headers: { 'x-test': 123 } }).success, false);
  const { Sandbox } = await import(pathToFileURL(sandboxEntry).href);
  assert.equal(typeof Sandbox.create, 'function');
  assert.equal(typeof Sandbox.get, 'function');
});
