// npm installs sigmastate-js 0.6.3 under the alias "sigmastate-js-next", but its typings declare the
// module "sigmastate-js/main". Reuse those typings for the alias.
declare module "sigmastate-js-next/main" {
  export * from "sigmastate-js/main";
}
