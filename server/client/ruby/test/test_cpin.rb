#! /usr/bin/env ruby
require 'rspec/autorun'
require_relative '../cpin'
require_relative './spec_helper'

module Candlepin
  describe "cpin" do
    context "in a unit context", :unit => true do
      it 'prints a usage message' do
        _, err = capture_io do
          expect do
            CPin.start(%w(foo))
          end.to raise_error(SystemExit)
        end
        expect(err).to match(/Commands:/)
      end
    end
  end
end
